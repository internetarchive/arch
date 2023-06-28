const DEBUG = false;

export enum Topics {
  API_SERVICE_READY = "API_SERVICE_READY",
}

export type TopicStrings = keyof typeof Topics;

// eslint-disable-next-line @typescript-eslint/ban-types
const topicSubscribersMap: Map<Topics, Function[]> = new Map(
  Object.values(Topics).map((topic) => [topic, []])
);

// eslint-disable-next-line @typescript-eslint/ban-types
export function topicSubscribers(topic: Topics): Function[] {
  return topicSubscribersMap.get(topic) || [];
}

function assertValidTopic(topic: TopicStrings): boolean {
  return Boolean(Topics[topic]);
}

export function subscribe<T>(topic: Topics, func: (props: T) => void): void {
  /* Subscribe func to the specified topic.
   */
  assertValidTopic(topic);
  // Do not subscribe func more than once.
  if (!topicSubscribers(topic).includes(func)) {
    topicSubscribers(topic).push(func);
    if (DEBUG) {
      console.debug(`${func.toString()} subscribed to ${topic}`);
    }
  }
}

// eslint-disable-next-line @typescript-eslint/ban-types
export function unsubscribe(topic: Topics, func: Function): void {
  /* Unsubscribe func from the specified topic.
   */
  assertValidTopic(topic);
  topicSubscribersMap.set(
    topic,
    topicSubscribers(topic).filter((f) => f !== func)
  );
}

// Define a monotonically increasing message ID integer.
let messageId = 0;

export async function publish<T>(topic: Topics, message: T): Promise<void> {
  /* Publish the specified message to all topic subscribers.
   */
  assertValidTopic(topic);
  messageId += 1;
  if (DEBUG) {
    console.debug(
      `Publishing to ${topicSubscribers(topic).length} subscribers ` +
        `on topic "${topic}" with message (id:${messageId}): ${JSON.stringify(
          message
        )}`
    );
  }
  await Promise.all(
    topicSubscribers(topic).map(
      (func) => new Promise((resolve) => resolve(func(message, messageId)))
    )
  );
}

// Convenience methods that expose subscribing and publishing with typed messages

// Factories for generating typed topic subscribers and publishers
export function _subscriberFactory<T>(
  topic: Topics
): (fn: (props: T) => void | Promise<void>) => void {
  return (fn: (props: T) => void) => subscribe<T>(topic, fn);
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function _publisherFactory<T extends Record<any, any>>(topic: Topics) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (props: T, errorHandler?: (error: any) => void | undefined) =>
    publish(topic, props)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .catch((error: any) => {
        if (errorHandler !== undefined) {
          errorHandler(error);
        } else {
          console.error(error);
        }
      });
}

// Message property types

export type ApiServiceReadyProps = Record<string, unknown>;

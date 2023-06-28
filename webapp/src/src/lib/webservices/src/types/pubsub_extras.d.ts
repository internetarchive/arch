import { TopicStrings } from "../lib/pubsub";

export {};

declare global {
  interface Window {
    // eslint-disable-next-line @typescript-eslint/ban-types
    topicSubscribersMap: Map<TopicStrings, Function[]>;
  }
}

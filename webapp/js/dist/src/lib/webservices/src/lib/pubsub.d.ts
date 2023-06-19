export declare enum Topics {
    API_SERVICE_READY = "API_SERVICE_READY"
}
export type TopicStrings = keyof typeof Topics;
export declare function topicSubscribers(topic: Topics): Function[];
export declare function subscribe<T>(topic: Topics, func: (props: T) => void): void;
export declare function unsubscribe(topic: Topics, func: Function): void;
export declare function publish<T>(topic: Topics, message: T): Promise<void>;
export declare function _subscriberFactory<T>(topic: Topics): (fn: (props: T) => void | Promise<void>) => void;
export declare function _publisherFactory<T extends Record<any, any>>(topic: Topics): (props: T, errorHandler?: ((error: any) => void | undefined) | undefined) => Promise<void>;
export type ApiServiceReadyProps = Record<string, unknown>;

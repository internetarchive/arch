import { LitElement, TemplateResult } from "lit";
type Constructor<T> = new (...args: any[]) => T;
export declare class ModalMixinInterface {
    _closeDialog(): void;
    _preCloseDialogEvent(): void;
    _postCloseDialogEvent(): void;
    _renderDialogContent(): TemplateResult;
    modalSize: string;
    scrollable: boolean;
}
export declare const ModalMixin: <T extends Constructor<LitElement>>(superClass: T) => Constructor<ModalMixinInterface> & T;
export {};

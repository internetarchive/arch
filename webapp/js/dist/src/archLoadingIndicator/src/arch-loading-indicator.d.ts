import { LitElement } from "lit";
export declare class ArchLoadingIndicator extends LitElement {
    text: string;
    static styles: import("lit").CSSResult;
    render(): import("lit-html").TemplateResult<1>;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-loading-indicator": ArchLoadingIndicator;
    }
}

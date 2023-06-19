import { LitElement } from "lit";
export declare enum AlertClass {
    Danger = "danger",
    Dark = "dark",
    Info = "info",
    Light = "light",
    Primary = "primary",
    Secondary = "secondary",
    Success = "success",
    Warning = "warning"
}
export declare class ArchAlert extends LitElement {
    alertClass: AlertClass;
    hidden: boolean;
    message: string;
    static styles: import("lit").CSSResult[];
    render(): import("lit-html").TemplateResult<1>;
    hide(): void;
    show(): void;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-alert": ArchAlert;
    }
}

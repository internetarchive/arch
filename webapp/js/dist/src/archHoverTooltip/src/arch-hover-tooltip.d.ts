import { LitElement } from "lit";
import "@spectrum-web-components/overlay/sync/overlay-trigger.js";
import "@spectrum-web-components/theme/sp-theme.js";
import "@spectrum-web-components/theme/src/themes.js";
import "@spectrum-web-components/tooltip/sp-tooltip.js";
export declare class ArchHoverTooltip extends LitElement {
    text: string;
    static styles: import("lit").CSSResult[];
    render(): import("lit-html").TemplateResult<1>;
    connectedCallback(): void;
    trySetupClickListener(): void;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-hover-tooltip": ArchHoverTooltip;
    }
}

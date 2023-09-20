import { LitElement } from "lit";
import "@spectrum-web-components/overlay/sync/overlay-trigger.js";
import "@spectrum-web-components/popover/sp-popover.js";
import "@spectrum-web-components/theme/sp-theme.js";
import "@spectrum-web-components/theme/src/themes.js";
export declare class ArchTooltip extends LitElement {
    header: string | undefined;
    text: string | undefined;
    learnMoreUrl: string | undefined;
    width: number;
    isOpen: boolean;
    static styles: import("lit").CSSResult[];
    render(): import("lit-html").TemplateResult<1>;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-tooltip": ArchTooltip;
    }
}

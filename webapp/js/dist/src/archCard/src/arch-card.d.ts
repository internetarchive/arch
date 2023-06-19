import { LitElement } from "lit";
export declare class ArchCard extends LitElement {
    title: string;
    headerLevel: number;
    ctaText: string | undefined;
    ctaHref: string | undefined;
    static styles: import("lit").CSSResult[];
    private get header();
    render(): import("lit-html").TemplateResult<1>;
}
declare global {
    interface HTMLElementTagNameMap {
        "arch-card": ArchCard;
    }
}

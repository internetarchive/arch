import { LitElement, html } from "lit";
import { customElement, property } from "lit/decorators.js";

import "../../archTooltip/index";

import styles from "./styles";

@customElement("arch-card")
export class ArchCard extends LitElement {
  @property({ type: String }) title = "Title";
  @property({ type: Number }) headerLevel = 2;
  @property({ type: String }) ctaText: string | undefined = undefined;
  @property({ type: String }) ctaHref: string | undefined = undefined;
  @property({ type: String }) ctaTooltipHeader: string | undefined = undefined;
  @property({ type: String }) ctaTooltipText: string | undefined = undefined;
  @property({ type: String }) ctaTooltipLearnMoreUrl: string | undefined =
    undefined;

  static styles = styles;

  private get header() {
    switch (this.headerLevel) {
      case 1:
        return html`<h1>${this.title}</h1>`;
        break;
      case 2:
        return html`<h2>${this.title}</h2>`;
        break;
      case 3:
        return html`<h3>${this.title}</h3>`;
        break;
      case 4:
        return html`<h4>${this.title}</h4>`;
        break;
      case 5:
        return html`<h5>${this.title}</h5>`;
        break;
      default:
        return html`<h6>${this.title}</h6>`;
        break;
    }
  }

  render() {
    const { ctaTooltipHeader, ctaTooltipText, ctaTooltipLearnMoreUrl } = this;
    return html`
      <section>
        <div class="header">
          ${this.header}
          ${!this.ctaText || !this.ctaHref
            ? ""
            : html`
                <a href="${this.ctaHref}">${this.ctaText}</a>
                ${!ctaTooltipHeader &&
                !ctaTooltipText &&
                !ctaTooltipLearnMoreUrl
                  ? html``
                  : html`
                      <arch-tooltip
                        .header=${ctaTooltipHeader}
                        .text=${ctaTooltipText}
                        .learnMoreUrl=${ctaTooltipLearnMoreUrl}
                      ></arch-tooltip>
                    `}
              `}
        </div>
        <hr />
        <slot name="content"></slot>
        <div class="footer">
          <slot name="footer"></slot>
        </div>
      </section>
    `;
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-card": ArchCard;
  }
}

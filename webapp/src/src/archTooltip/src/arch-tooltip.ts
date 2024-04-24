import { LitElement, html } from "lit";
import { customElement, property, state } from "lit/decorators.js";

import "@spectrum-web-components/overlay/sync/overlay-trigger.js";
import "@spectrum-web-components/popover/sp-popover.js";
import "@spectrum-web-components/theme/sp-theme.js";
import "@spectrum-web-components/theme/src/themes.js";

import styles from "./styles";

@customElement("arch-tooltip")
export class ArchTooltip extends LitElement {
  @property({ type: String }) header: string | undefined = undefined;
  @property({ type: String }) text: string | undefined = undefined;
  @property({ type: String }) learnMoreUrl: string | undefined = undefined;
  @property({ type: Number }) width = 12;

  @state() isOpen = false;

  static styles = styles;

  render() {
    const { header, text, learnMoreUrl, width } = this;
    return html`
      <sp-theme color="light" scale="medium">
        <overlay-trigger
          placement="bottom-end"
          type="inline"
          @sp-opened=${() => (this.isOpen = true)}
          @sp-closed=${() => (this.isOpen = false)}
        >
          <button
            class="text"
            slot="trigger"
            title="Learn about ${this.header}"
          >
            ${this.isOpen ? html`<strong>&#9432;</strong>` : html`&#9432;`}
          </button>
          <sp-popover slot="click-content" position="bottom" tip open>
            <div role="dialog">
              ${!header
                ? html``
                : html` <h2><span>&#9432;</span>${header}</h2>`}
              <div class="text" style="width: ${width}rem">${text || ""}</div>
              ${!learnMoreUrl
                ? html``
                : html`
                    <div class="learn-more">
                      <a href="${learnMoreUrl}" target="_blank">Learn More</a>
                    </div>
                  `}
            </div>
          </sp-popover>
        </overlay-trigger>
      </sp-theme>
    `;
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-tooltip": ArchTooltip;
  }
}

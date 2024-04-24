import { LitElement, html } from "lit";
import { customElement, property } from "lit/decorators.js";

import "@spectrum-web-components/overlay/sync/overlay-trigger.js";
import "@spectrum-web-components/theme/sp-theme.js";
import "@spectrum-web-components/theme/src/themes.js";
import "@spectrum-web-components/tooltip/sp-tooltip.js";

import styles from "./styles";

@customElement("arch-hover-tooltip")
export class ArchHoverTooltip extends LitElement {
  @property({ type: String }) text = "";

  static styles = styles;

  render() {
    const { text } = this;
    return html`
      <sp-theme color="light" scale="medium">
        <overlay-trigger placement="top" type="inline">
          <span slot="trigger"><slot></slot></span>
          <sp-tooltip slot="hover-content">${text}</sp-tooltip>
        </overlay-trigger>
      </sp-theme>
    `;
  }

  connectedCallback() {
    super.connectedCallback();
    this.trySetupClickListener();
  }

  trySetupClickListener() {
    /* The Spectrum overlay-trigger element swallows click events,
       so let's hijack these events from its trigger element and
       propagate them to our parent.
    */
    const el = this.shadowRoot
      ?.querySelector("overlay-trigger")
      ?.shadowRoot?.querySelector("#trigger");
    if (el) {
      el.addEventListener(
        "click",
        () => {
          this.parentElement?.click();
        },
        true
      );
    } else {
      setTimeout(this.trySetupClickListener.bind(this), 100);
    }
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-hover-tooltip": ArchHoverTooltip;
  }
}

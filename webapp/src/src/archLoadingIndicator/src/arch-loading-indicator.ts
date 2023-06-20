import { LitElement, html } from "lit";
import { customElement, property } from "lit/decorators.js";

import styles from "./styles";

@customElement("arch-loading-indicator")
export class ArchLoadingIndicator extends LitElement {
  @property({ type: String }) text = "Loading";

  static styles = styles;

  render() {
    // Animation source: https://loading.io/css/
    const { text } = this;
    return html`
      ${text}
      <span class="la-ball-pulse">
        <span>&#x2B24;</span>
        <span>&#x2B24;</span>
        <span>&#x2B24;</span>
      </div>
    `;
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-loading-indicator": ArchLoadingIndicator;
  }
}

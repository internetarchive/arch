import { LitElement, html } from "lit";
import { customElement, property } from "lit/decorators.js";

import { AvailableJobs, JobState } from "../../lib/types";

import "./arch-job-card";

@customElement("arch-job-category-section")
export class ArchJobCategorySection extends LitElement {
  @property({ type: Boolean }) collapsed = false;
  @property({ type: String }) collectionId!: string;
  @property({ type: Object }) jobsCat!: AvailableJobs[0];
  @property({ type: Object }) jobStates!: Record<string, JobState>;

  createRenderRoot() {
    /* Disable the shadow root for this component to let in global styles.
       https://stackoverflow.com/a/55213037 */
    return this;
  }

  connectedCallback() {
    this.setAttribute("aria-controls", this.jobsCat.categoryName);
    super.connectedCallback();
  }

  expand() {
    this.collapsed = false;
  }

  collapse() {
    this.collapsed = true;
  }

  render() {
    return html`
      <div
        class="job-category ${this.collapsed ? "collapsed" : "expanded"}"
        aria-expanded="${this.collapsed ? "false" : "true"}"
      >
        <div class="category-wrapper">
          <img
            class="category-image"
            src="${this.jobsCat.categoryImage}"
            alt="Icon for ${this.jobsCat.categoryName}"
          />
          <h2 id="${this.jobsCat.categoryId}" class="category-title">
            ${this.jobsCat.categoryName}
          </h2>
          <p>${this.jobsCat.categoryDescription}</p>
        </div>
        <div class="collapsible-content">
          ${this.jobsCat.jobs.map(
            (job) => html`
              <arch-job-card
                .collectionId=${this.collectionId}
                .job=${job}
                .jobStates=${this.jobStates}
              >
              </arch-job-card>
            `
          )}
        </div>
      </div>
    `;
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-job-category-section": ArchJobCategorySection;
  }
}

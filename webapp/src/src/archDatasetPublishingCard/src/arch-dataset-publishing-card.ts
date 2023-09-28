import { LitElement, html } from "lit";
import { customElement, property, state, query } from "lit/decorators.js";

import { isoStringToDateString } from "../../lib/helpers";
import {
  Collection,
  Job,
  PublishedDatasetInfo,
  PublishedDatasetMetadata,
  PublishedDatasetMetadataApiResponse,
  PublishedDatasetMetadataJSONSchema,
  PublishedDatasetMetadataJSONSchemaProps,
} from "../../lib/types";
import "../../archLoadingIndicator/index";
import "../../archDatasetMetadataForm/index";
import { ArchDatasetMetadataForm } from "../../archDatasetMetadataForm/index";

import styles from "./styles";
import * as _metadataSchema from "../../archDatasetMetadataForm/src/schema.json";

const metadataSchema = _metadataSchema as PublishedDatasetMetadataJSONSchema;
const propertiesOrder = metadataSchema.propertiesOrder as Array<string>;

enum PublishState {
  Loading = 0,
  Unpublished,
  PrePublish,
  Publishing,
  Published,
  Unpublishing,
}

enum MetadataState {
  Displaying,
  Editing,
  Saving,
}

const orderedMetadataKeys = Object.keys(
  metadataSchema.properties as object
).sort((a, b) =>
  propertiesOrder.indexOf(a) < propertiesOrder.indexOf(b) ? -1 : 1
) as Array<keyof PublishedDatasetMetadata>;

function getMetadataKeyTitle(k: keyof PublishedDatasetMetadata): string {
  // Note that I can't figure out how to properly annotate metadataSchema.properties
  // using something like PropertiesSchema from ajv/lib/types/json-schema.
  return (metadataSchema.properties as PublishedDatasetMetadataJSONSchemaProps)[
    k
  ].title as string;
}

@customElement("arch-dataset-publishing-card")
export class ArchDatasetPublishingCard extends LitElement {
  @property({ type: String }) collectionId!: Collection["id"];
  @property({ type: String }) jobId!: Job["id"];
  @property({ type: Boolean }) isSample!: boolean;

  @state() pubState: PublishState = PublishState.Loading;
  @state() pubInfo: undefined | PublishedDatasetInfo = undefined;

  @state() metadataState = MetadataState.Displaying;
  @state() metadata: undefined | PublishedDatasetMetadataApiResponse =
    undefined;

  @query("arch-dataset-metadata-form") metadataForm!: ArchDatasetMetadataForm;

  static styles = styles;

  connectedCallback() {
    super.connectedCallback();
    void this._fetchInitialData();
  }

  private get _sampleParam() {
    /* Return a "sample=[true|false]" URL param string for the current isSample state */
    return `sample=${this.isSample ? "true" : "false"}`;
  }

  private get _metadataFormData() {
    /* Return the metadata <form> inputs as an object with Array-type values. */
    const metadata: PublishedDatasetMetadataApiResponse = {};
    const metadataPairs = Array.from(
      new FormData(this.metadataForm.form).entries()
    )
      // Remove empty string values.
      .filter(([, v]) => (v as string).trim() !== "")
      // Replace any tabs with " " and "\n" with "<br>", which should only ever
      // occur in the case of <textarea>.
      .map(([k, v]) => [
        k,
        (v as string).replaceAll("\t", " ").replaceAll("\n", "<br>"),
      ]) as Array<[keyof PublishedDatasetMetadataApiResponse, string]>;

    for (const [name, value] of metadataPairs) {
      metadata[name] = (metadata[name] ?? []).concat(value);
    }
    return metadata;
  }

  // TODO - make this less complex
  // eslint-disable-next-line complexity
  render() {
    const { pubState } = this;
    if (pubState === PublishState.Loading) {
      return html`<arch-loading-indicator></arch-loading-indicator>`;
    }
    const { metadata } = this;
    const pubInfo = this.pubInfo as PublishedDatasetInfo;
    return html`
      <div class="container">
        <div class="detail">
          <dl>
            <div>
              <dt>Last Published</dt>
              <dd>
                ${pubState === PublishState.Published
                  ? isoStringToDateString(pubInfo.time)
                  : "never"}
              </dd>
            </div>
            ${pubState !== PublishState.Published
              ? html``
              : html`
                  <div>
                    <dt>ARK</dt>
                    <dd>
                      <a href="https://ark.archive.org/${pubInfo.ark}"
                        >${pubInfo.ark}</a
                      >
                    </dd>
                  </div>
                `}
          </dl>

          <!-- Metadata section header -->
          <h2>
            ${pubState < PublishState.PrePublish ||
            pubState === PublishState.Publishing
              ? ""
              : pubState === PublishState.PrePublish
              ? html`<i>Enter Metadata</i>`
              : "Metadata"}
            ${pubState < PublishState.Published ||
            this.metadataState === MetadataState.Editing
              ? ""
              : html`
                  <button
                    class="text"
                    @click=${() => (this.metadataState = MetadataState.Editing)}
                  >
                    (edit)
                  </button>
                `}
          </h2>

          <!-- Metadata display list -->
          <div
            class="metadata-display"
            ?hidden=${pubState < PublishState.Published ||
            this.metadataState === MetadataState.Editing}
          >
            ${metadata === undefined
              ? html`<arch-loading-indicator></arch-loading-indicator>`
              : Object.keys(metadata).length === 0
              ? html`<i>none</i>`
              : html`
                  <dl>
                    ${orderedMetadataKeys
                      .filter((k) => metadata[k] !== undefined)
                      .map((k) => {
                        const title = getMetadataKeyTitle(k);
                        let values = metadata[k] as string | Array<string>;
                        if (!Array.isArray(values)) {
                          values = [values];
                        }
                        return html`
                          <div>
                            <dt>${title}</dt>
                            ${values.map((value) => html`<dd>${value}</dd>`)}
                          </div>
                        `;
                      })}
                  </dl>
                `}
          </div>

          <!-- Metadata edit form -->
          <div
            class="metadata-edit"
            ?hidden=${pubState !== PublishState.PrePublish &&
            this.metadataState !== MetadataState.Editing &&
            this.metadataState !== MetadataState.Saving}
          >
            ${pubState !== PublishState.PrePublish &&
            this.metadataState !== MetadataState.Editing &&
            this.metadataState !== MetadataState.Saving
              ? html``
              : html`
                  <arch-dataset-metadata-form
                    metadata="${JSON.stringify(metadata ?? "")}"
                  >
                  </arch-dataset-metadata-form>
                `}
            <br />
            <div
              ?hidden=${pubState === PublishState.PrePublish}
              class="form-buttons"
            >
              <button
                type="button"
                @click=${() => (this.metadataState = MetadataState.Displaying)}
                ?disabled=${this.metadataState === MetadataState.Saving}
              >
                Cancel
              </button>
              <button
                type="button"
                class="primary"
                @click=${() => this._saveMetadata()}
                ?disabled=${this.metadataState === MetadataState.Saving}
              >
                ${this.metadataState === MetadataState.Saving
                  ? html`<arch-loading-indicator
                      style="--color: #fff"
                      text="Saving"
                    ></arch-loading-indicator>`
                  : html`Save`}
              </button>
            </div>
          </div>
        </div>

        <button
          class="cancel"
          @click=${() => (this.pubState = PublishState.Unpublished)}
          ?hidden=${pubState !== PublishState.PrePublish}
        >
          Cancel
        </button>

        <button
          class="${pubState === PublishState.Unpublished
            ? "primary"
            : pubState === PublishState.PrePublish
            ? "success"
            : pubState === PublishState.Published
            ? "danger"
            : ""}"
          ?disabled=${pubState === PublishState.Publishing ||
          pubState === PublishState.Unpublishing}
          @click=${this._publishButtonClickHandler}
        >
          ${pubState === PublishState.Unpublished
            ? "Publish"
            : pubState === PublishState.PrePublish
            ? "Publish Now"
            : pubState === PublishState.Publishing
            ? "Publish in progress..."
            : pubState === PublishState.Published
            ? "Unpublish"
            : pubState === PublishState.Unpublishing
            ? "Unpublishing..."
            : ""}
        </button>
      </div>
    `;
  }

  private async _fetchInitialData() {
    // Fetch any existing publication info.
    const pubInfo = await this._fetchPubInfo();
    if (!pubInfo) {
      // No publication job exists for this dataset.
      this.pubState = PublishState.Unpublished;
      this.metadata = {};
      return;
    }
    // Check whether the job is in progress.
    if (pubInfo.complete === false) {
      this.pubState = PublishState.Publishing;
      // Check again for published info in 3 seconds.
      setTimeout(() => void this._fetchInitialData(), 3000);
      return;
    }
    // Dataset has been published.
    this.pubInfo = pubInfo;
    this.pubState = PublishState.Published;
    // Fetch the published metadata.
    void this._pollItemMetadata();
  }

  private async _pollItemMetadata() {
    /* Poll for the item metadata and save it once available. */
    const { pubState } = this;
    const pubInfo = this.pubInfo as PublishedDatasetInfo;
    const metadata = await this._fetchItemMetadata(pubInfo.item);
    if (metadata === undefined && pubState === PublishState.Published) {
      // Try again in 3 seconds.
      setTimeout(() => void this._pollItemMetadata(), 3000);
    }
    // Got it.
    this.metadata = metadata;
  }

  private async _fetchPubInfo() {
    /* Attempt to retrieve the info for any existing published dataset */
    const response = await fetch(
      `/api/petabox/${this.collectionId}/${this.jobId}?${this._sampleParam}`
    );
    if (response.status === 404) {
      return undefined;
    } else {
      const pubInfo = (await response.json()) as PublishedDatasetInfo;
      // Convert datetime string to Date.
      pubInfo.time = new Date(pubInfo.time);
      return pubInfo;
    }
  }

  private async _fetchItemMetadata(itemId: PublishedDatasetInfo["item"]) {
    /* Attempt to retrieve the published item metadata */
    const response = await fetch(
      `/api/petabox/${this.collectionId}/metadata/${itemId}`
    );
    if (response.status === 404) {
      return undefined;
    }
    return (await response.json()) as PublishedDatasetMetadataApiResponse;
  }

  private _publishButtonClickHandler() {
    const metadataForm = this.metadataForm;
    switch (this.pubState) {
      case PublishState.Unpublished:
        this.pubState = PublishState.PrePublish;
        break;
      case PublishState.PrePublish:
        if (metadataForm.form.checkValidity()) {
          void this._publish();
        } else {
          metadataForm.form.reportValidity();
        }
        break;
      case PublishState.Published:
        if (
          window.confirm("Are you sure you want to unpublish this dataset?")
        ) {
          void this._unpublish();
        }
        break;
    }
  }

  private async _publish() {
    const { collectionId, jobId, _metadataFormData: metadata } = this;
    await fetch(
      `/api/runjob/DatasetPublication/${collectionId}?${this._sampleParam}`,
      {
        method: "POST",
        credentials: "same-origin",
        mode: "cors",
        body: JSON.stringify({
          dataset: jobId,
          metadata,
        }),
      }
    );
    this.pubState = PublishState.Publishing;
    // Start polling for pub info after a lengthy timeout in order to
    // give the backend time to register the job.
    setTimeout(() => void this._fetchInitialData(), 30000);
  }

  private async _unpublish() {
    const { collectionId, pubInfo } = this;
    const { item: itemId } = pubInfo as PublishedDatasetInfo;
    this.pubState = PublishState.Unpublishing;
    await fetch(`/api/petabox/${collectionId}/delete/${itemId}`, {
      method: "POST",
      credentials: "same-origin",
      mode: "cors",
      body: JSON.stringify({ delete: true }),
    });
    this.pubState = PublishState.Unpublished;
    // Call fetchInitialData to reset the component state.
    void this._fetchInitialData();
  }

  private async _saveMetadata() {
    const { collectionId, pubInfo, _metadataFormData: metadata } = this;
    const { item: itemId } = pubInfo as PublishedDatasetInfo;
    this.metadata = metadata;
    this.metadataState = MetadataState.Saving;
    // Add empty array values for all unspecified metadata fields in order to delete
    // any existing values from the item.
    const finalMetadata = Object.assign(
      Object.fromEntries(orderedMetadataKeys.map((k) => [k, []])),
      metadata
    );
    await fetch(`/api/petabox/${collectionId}/metadata/${itemId}`, {
      method: "POST",
      credentials: "same-origin",
      mode: "cors",
      body: JSON.stringify(finalMetadata),
    });
    this.metadataState = MetadataState.Displaying;
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-dataset-publishing-card": ArchDatasetPublishingCard;
  }
}

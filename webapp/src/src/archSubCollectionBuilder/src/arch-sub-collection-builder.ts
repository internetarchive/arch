import { LitElement, html } from "lit";
import { customElement, query, state } from "lit/decorators.js";

import { Collection } from "../../lib/types";
import { AlertClass } from "../../archAlert/index";
import "../../archAlert/index";

@customElement("arch-sub-collection-builder")
export class ArchSubCollectionBuilder extends LitElement {
  @state() collections: Array<Collection> = [];
  @state() sourceCollectionIds: Set<Collection["id"]> = new Set();

  @query("form") form!: HTMLFormElement;
  @query("select#source") sourceSelect!: HTMLSelectElement;

  static urlCollectionsParamName = "cid[]";

  createRenderRoot() {
    /* Disable the shadow root for this component to let in global styles.
       https://stackoverflow.com/a/55213037 */
    return this;
  }

  async connectedCallback() {
    super.connectedCallback();

    // Fetch available Collections and Jobs.
    await this.initCollections();
    // Select any initial Collections.
    this.sourceCollectionIds = new Set(
      new URLSearchParams(window.location.search).getAll(
        ArchSubCollectionBuilder.urlCollectionsParamName
      )
    );
  }

  private get _formData() {
    /* Return the <form> inputs as an object with potential Array-type values. */
    const data: Record<string, string | Array<string>> = {};
    const pairs = Array.from(new FormData(this.form).entries()) as Array<
      [string, string]
    >;
    for (const [name, value] of pairs) {
      const existingValue = data[name];
      if (existingValue === undefined) {
        data[name] = value;
      } else if (!Array.isArray(existingValue)) {
        data[name] = [existingValue, value];
      } else {
        data[name] = existingValue.concat(value);
      }
    }
    return data;
  }

  render() {
    const { collections, sourceCollectionIds } = this;
    const sourceCollections = collections.filter((x) =>
      sourceCollectionIds.has(x.id)
    );
    return html`
      <div class="row">
        <div class="large-12 columns">
          <arch-alert
            .alertClass=${AlertClass.Primary}
            .message=${'Use this form to create a custom collection by filtering the contents of one or more existing source collections. You may use as many of the filtering options below as you desire and leave others blank. <a href="https://arch-webservices.zendesk.com/hc/en-us/articles/16107865758228" target="_blank">Learn about options and common choices here</a>. ARCH will email you when your custom collection is ready to use.'}
          >
          </arch-alert>

          <form>
            <label for="sources" class="required"> Source Collection(s) </label>
            <em id="sourceDesc">
              Select the collection(s) to use as the source for this custom
              collection.
            </em>
            <select
              name="sources"
              id="sources"
              aria-labelledby="source sourceDesc"
              required
              multiple
              size="8"
              style="resize: vertical;"
              ?disabled=${this.collections.length === 0}
              @change=${this.sourceCollectionsChangeHandler}
            >
              ${this.collections.length === 0
                ? html`<option value="">Loading Collections...</option>`
                : html``}
              ${collections.map(
                (collection) => html`
                  <option
                    value="${collection.id}"
                    ?selected=${sourceCollectionIds.has(collection.id)}
                  >
                    ${collection.name}
                  </option>
                `
              )}
            </select>

            <label for="name" class="required"> Custom Collection Name </label>
            <em id="nameDesc">
              Give your custom collection a name to describe its contents.
            </em>
            <input
              type="text"
              name="name"
              id="name"
              aria-labelledby="name nameDesc"
              placeholder="${sourceCollections.length > 0
                ? sourceCollections[0].name
                : "Example Collection"} - My filters"
              required
            />

            <label for="surts"> SURT Prefix(es) </label>
            <em id="surtsDesc">
              Choose
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#document"
                target="_blank"
                >web documents</a
              >
              to include in your custom collection by their
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#surt"
                target="_blank"
                >SURT prefix/es</a
              >. Separate multiple SURTs with a <code>|</code> character and no
              space in-between.
            </em>
            <input
              type="text"
              name="surtPrefixesOR"
              id="surts"
              aria-labelledby="surts surtsDesc"
              placeholder="org,archive|gov,congress)/committees"
            />

            <label for="timestampFrom"> Crawl Date (start) </label>
            <em id="timestampFromDesc">
              Specify the earliest in a range of
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#timestamp"
                target="_blank"
                >timestamps</a
              >
              to include in your custom collection. Enter a full timestamp (in
              the <code>yyyyMMddHHmmSS</code> format), a prefix (ex.
              <code>yyyyMM</code>), or leave blank to include all web documents
              going back to the earliest collected.
            </em>
            <input
              type="text"
              name="timestampFrom"
              id="timestampFrom"
              aria-labelledby="timestampFrom timestampFromDesc"
              placeholder="19960115"
            />

            <label for="timestampTo"> Crawl Date (end) </label>
            <em id="timestampToDesc">
              Specify the latest in a range of
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#timestamp"
                target="_blank"
                >timestamps</a
              >
              to include in your custom collection. Enter a full timestamp (in
              the <code>yyyyMMddHHmmSS</code> format), a prefix (ex.
              <code>yyyyMM</code>), or leave blank to include all web documents
              up to the most recent collected.
            </em>
            <input
              type="text"
              name="timestampTo"
              id="timestampTo"
              aria-labelledby="timestampTo timestampToDesc"
              placeholder="19991231235959"
            />

            <label for="status"> HTTP Status </label>
            <em id="statusDesc">
              Choose web documents to include in your custom collection by their
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#status"
                target="_blank"
                >HTTP status code/s</a
              >. Separate multiple status codes with a <code>|</code> character
              and no space in-between.
            </em>
            <input
              type="text"
              name="statusPrefixesOR"
              id="status"
              aria-labelledby="status statusDesc"
              placeholder="200"
            />

            <label for="mime"> MIME Type </label>
            <em id="mimeDesc">
              Choose web documents to include in your custom collection by their
              file format/s, expressed as
              <a
                href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#mime"
                target="_blank"
                >MIME type/s</a
              >. Separate multiple MIME types with a | character and no space
              in-between.
            </em>
            <input
              type="text"
              name="mimesOR"
              id="mime"
              aria-labelledby="mime mimeDesc"
              placeholder="text/html|application/pdf"
            />

            <button type="submit" @click=${this.createSubCollection}>
              Create Custom Collection
            </button>
          </form>
        </div>
      </div>
    `;
  }

  private async initCollections() {
    this.collections = (await (
      await fetch("/api/collections")
    ).json()) as Array<Collection>;
  }

  private setSourceCollectionIdsUrlParam(
    collectionIds: Array<Collection["id"]>
  ) {
    const { urlCollectionsParamName } = ArchSubCollectionBuilder;
    const url = new URL(window.location.href);
    // Unconditionally delete the params in preparation for any params.append()
    url.searchParams.delete(urlCollectionsParamName);
    collectionIds.forEach((collectionId) =>
      url.searchParams.append(urlCollectionsParamName, collectionId)
    );
    history.replaceState(null, "", url.toString());
  }

  private sourceCollectionsChangeHandler(e: Event) {
    const collectionIds = Array.from(
      (e.target as HTMLSelectElement).selectedOptions
    ).map((el) => el.value);
    this.sourceCollectionIds = new Set(collectionIds);
    this.setSourceCollectionIdsUrlParam(collectionIds);
  }

  private async createSubCollection(e: Event) {
    // Prevent the form submission.
    e.preventDefault();
    // Check form validity and abort if invalid.
    const { form } = this;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    // Disable the button.
    const button = e.target as HTMLButtonElement;
    button.disabled = true;

    // Read and filter the form data.
    const data = Object.fromEntries(
      Array.from(Object.entries(this._formData))
        // Remove empty fields
        .filter(([, v]) => v !== "")
        // Split pipe-delimited multi-valued strings into arrays.
        .map(([k, v]) => [
          k,
          k === "surtPrefixesOR" || k === "statusPrefixesOR" || k === "mimesOR"
            ? (v as string).split("|")
            : v,
        ])
    );

    // Pop data.sources, which will either be a string (for a single selection)
    // or an string[] (for multiple selections).
    const sources = data.sources;
    delete data.sources;
    // Handle single vs. multiple source collection cases.
    let urlCollectionId;
    if (!Array.isArray(sources)) {
      // Specify the single Collection ID as the url param.
      urlCollectionId = sources;
    } else {
      // Specify the special "UNION-UDQ" Collection ID as the url param.
      urlCollectionId = "UNION-UDQ";
      // Assign sources to data.input which is expected by
      // DerivationJobConf.jobInPath() for Union-type collections.
      data.input = sources;
    }

    // Make the request.
    const res = await fetch(`/api/runjob/UserDefinedQuery/${urlCollectionId}`, {
      method: "POST",
      body: JSON.stringify(data),
      headers: {
        "content-type": "application/json",
      },
    });
    if (res.ok) {
      // Request was successful - redirect to collections table.
      window.location.href = "/collections";
    } else {
      // Display an alert.
      window.alert("Could not create Sub-Collection");
      // Re-enable the button.
      button.disabled = false;
    }
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-sub-collection-builder": ArchSubCollectionBuilder;
  }
}

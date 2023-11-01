import { LitElement, html } from "lit";
import { customElement, query, state } from "lit/decorators.js";

import ArchAPI from "../../lib/ArchAPI";
import { HtmlStatusCodeRegex, SurtPrefixRegex } from "../../lib/constants";
import { Collection, FilteredApiResponse } from "../../lib/types";
import { AlertClass } from "../../archAlert/index";
import "../../archAlert/index";

import styles from "./styles";
import { DecodedFormData } from "./types";

// https://www.iana.org/assignments/media-types/media-types.xhtml
import ValidMediaTypeSubTypesMap from "./mediaTypes.js";

/*
 * Helpers
 */

function splitFieldValue(s: string): Array<string> {
  return s
    .split("|")
    .map((x) => x.trim())
    .filter((x) => x !== "");
}

function parseDatetimeFieldValue(s: string): string {
  // Convert datetime-local input string (yyyy-MM-ddTHH:mm) to timestamp string (yyyyMMddHHmmSS).
  // The form input only provides for minute resolution, so append 00 for the seconds.
  return s === "" ? s : s.replaceAll(/[^\d]/g, "") + "00";
}

/*
 * ArchSubCollectionBuilder Class
 */

@customElement("arch-sub-collection-builder")
export class ArchSubCollectionBuilder extends LitElement {
  @state() collections: Array<Collection> = [];
  @state() sourceCollectionIds: Set<Collection["id"]> = new Set();

  @query("form") form!: HTMLFormElement;
  @query("select#source") sourceSelect!: HTMLSelectElement;

  static styles = styles;
  static urlCollectionsParamName = "cid[]";

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

  render() {
    const { collections, sourceCollectionIds } = this;
    const sourceCollections = collections.filter((x) =>
      sourceCollectionIds.has(x.id)
    );
    return html`
      <arch-alert
        .alertClass=${AlertClass.Primary}
        .message=${'Use this form to create a custom collection by filtering the contents of one or more existing source collections. You may use as many of the filtering options below as you desire and leave others blank. <a href="https://arch-webservices.zendesk.com/hc/en-us/articles/16107865758228" target="_blank">Learn about options and common choices here</a>. ARCH will email you when your custom collection is ready to use.'}
      >
      </arch-alert>

      <form @input=${this.inputHandler}>
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
          >.
          <br />
          Separate multiple SURTs with a <code>|</code> character and no space
          in-between.
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
          to include in your custom collection, or leave blank to include all
          web documents going back to the earliest collected.
        </em>
        <input
          type="datetime-local"
          name="timestampFrom"
          id="timestampFrom"
          aria-labelledby="timestampFrom timestampFromDesc"
        />

        <label for="timestampTo"> Crawl Date (end) </label>
        <em id="timestampToDesc">
          Specify the latest in a range of
          <a
            href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#timestamp"
            target="_blank"
            >timestamps</a
          >
          to include in your custom collection, or leave blank to include all
          web documents up to the most recent collected.
        </em>
        <input
          type="datetime-local"
          name="timestampTo"
          id="timestampTo"
          aria-labelledby="timestampTo timestampToDesc"
        />

        <label for="status"> HTTP Status </label>
        <em id="statusDesc">
          Choose web documents to include in your custom collection by their
          <a
            href="https://arch-webservices.zendesk.com/hc/en-us/articles/14410683244948#status"
            target="_blank"
            >HTTP status code/s</a
          >.
          <br />
          Separate multiple HTTP Status values with a <code>|</code> character
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
          >.
          <br />
          Separate multiple MIMEs with a <code>|</code> character and no space
          in-between.
        </em>
        <input
          type="text"
          name="mimesOR"
          id="mime"
          aria-labelledby="mime mimeDesc"
          placeholder="text/html|application/pdf"
        />
        <br />
        <button
          type="submit"
          class="primary"
          @click=${this.createSubCollection}
        >
          Create Custom Collection
        </button>
      </form>
    `;
  }

  private inputHandler(e: Event) {
    // Clear any custom validity message on input value change.
    (e.target as HTMLInputElement | HTMLSelectElement).setCustomValidity("");
  }

  private async initCollections() {
    const response =
      (await ArchAPI.collections.get()) as FilteredApiResponse<Collection>;
    this.collections = response.results;
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

  private static fieldValueParserMap: Record<
    string,
    ((s: string) => string) | ((s: string) => Array<string>)
  > = {
    mimesOR: splitFieldValue,
    statusPrefixesOR: splitFieldValue,
    surtPrefixesOR: splitFieldValue,
    timestampFrom: parseDatetimeFieldValue,
    timestampTo: parseDatetimeFieldValue,
  };

  private static fieldValueValidatorMessagePairMap: Record<
    string,
    [(s: string) => boolean, string]
  > = {
    statusPrefixesOR: [
      (s) => HtmlStatusCodeRegex.test(s),
      "Please correct the invalid status code(s)",
    ],
    surtPrefixesOR: [
      (s) => SurtPrefixRegex.test(s),
      "Please correct the invalid SURT(s)",
    ],
    mimesOR: [
      (s) => {
        const splits = s.split("/");
        return (
          splits.length === 2 &&
          (ValidMediaTypeSubTypesMap as Record<string, Array<string>>)[
            splits[0]
          ]?.includes(splits[1])
        );
      },
      "Please correct the invalid MIME(s)",
    ],
  };

  private static decodeFormDataValue(
    k: string,
    v: string
  ): string | Array<string> | Error {
    let rv: string | Array<string> | Error = v;
    // If a parser is defined, apply it.
    const parse = ArchSubCollectionBuilder.fieldValueParserMap[k];
    if (parse !== undefined) {
      rv = parse(rv);
    }
    // If a validator is defined, apply it.
    const isValidMessagePair =
      ArchSubCollectionBuilder.fieldValueValidatorMessagePairMap[k];
    if (isValidMessagePair !== undefined) {
      const [isValid, message] = isValidMessagePair;
      const badVals = (Array.isArray(rv) ? rv : [rv]).filter(
        (s) => !isValid(s)
      );
      if (badVals.length > 0) {
        rv = new Error(`${message}: ${badVals.join(", ")}`);
      }
    }
    return rv;
  }

  private static validateDecodedFormData(
    data: DecodedFormData
  ): DecodedFormData {
    // If a timestampFrom/To pair is invalid, add a validation error to the ...To field.
    if (
      typeof data.timestampFrom === "string" &&
      typeof data.timestampTo === "string" &&
      data.timestampFrom >= data.timestampTo
    ) {
      data.timestampTo = new Error(
        "Crawl Date (end) must be later than Crawl Date (start)"
      );
    }
    return data;
  }

  private get formData(): DecodedFormData {
    // Return the <form> inputs as a validated, API POST-ready object.
    const formData = new FormData(this.form);
    let data = Object.fromEntries(
      Array.from(new Set(formData.keys()).values()) // use Set to dedupe keys
        .map((k) => [k, k === "sources" ? formData.getAll(k) : formData.get(k)])
        // Convert the form input strings to their API POST-ready values.
        .map(([k, v]) => [
          k,
          // Sources may be an Array<string>, and requires no parsing, so special-case it.
          k === "sources"
            ? (v as Array<string>)
            : ArchSubCollectionBuilder.decodeFormDataValue(
                k as string,
                v as string
              ),
        ])
        // Remove fields with an empty string or Array value.
        .filter(
          ([, v]) =>
            v instanceof Error || (v as string | Array<string>).length > 0
        )
    ) as DecodedFormData;
    data = ArchSubCollectionBuilder.validateDecodedFormData(data);
    return data;
  }

  private setFormInputValidity(data: DecodedFormData) {
    // Set or clear each form <input>'s validity based on whether a decoding
    // attempt failed.
    for (const [k, v] of Object.entries(data)) {
      if (k !== "sources") {
        (
          this.form.querySelector(`input[name="${k}"]`) as HTMLInputElement
        ).setCustomValidity(v instanceof Error ? v.message : "");
      }
    }
  }

  private async doPost(
    urlCollectionId: string,
    data: Record<string, string | Array<string>>
  ) {
    return fetch(`/api/runjob/UserDefinedQuery/${urlCollectionId}`, {
      method: "POST",
      body: JSON.stringify(data),
      headers: {
        "content-type": "application/json",
      },
    });
  }

  private async createSubCollection(e: Event) {
    // Prevent the form submission.
    e.preventDefault();
    // Disable the submit button.
    const button = e.target as HTMLButtonElement;
    button.disabled = true;

    // Read and filter the form data.
    const formData = this.formData;

    // Update form field validity.
    this.setFormInputValidity(formData);

    // Check form validity and abort if invalid.
    const { form } = this;
    if (!form.checkValidity()) {
      form.reportValidity();
      // Re-enable the submit button.
      button.disabled = false;
      return;
    }

    // Pop data.sources, which will either be a string (for a single selection)
    // or an string[] (for multiple selections).
    const sources = formData.sources as Array<string>;
    // Create a new data object that we'll mold into shape.
    const finalData = Object.assign({}, formData) as Record<
      string,
      string | Array<string>
    >;
    delete finalData.sources;
    // Handle single vs. multiple source collection cases.
    let urlCollectionId;
    if (sources.length === 1) {
      // Specify the single Collection ID as the url param.
      urlCollectionId = sources[0];
    } else {
      // Specify the special "UNION-UDQ" Collection ID as the url param.
      urlCollectionId = "UNION-UDQ";
      // Assign sources to data.input which is expected by
      // DerivationJobConf.jobInPath() for Union-type collections.
      finalData.input = sources;
    }

    // Make the request.
    const res = await this.doPost(urlCollectionId, finalData);
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

import Ajv from "ajv";
import { SomeJSONSchema } from "ajv/lib/types/json-schema";
import { LitElement, PropertyValues, html } from "lit";
import { customElement, property, query, queryAll } from "lit/decorators.js";

import {
  PublishedDatasetMetadata,
  PublishedDatasetMetadataValue,
  PublishedDatasetMetadataJSONSchema,
  PublishedDatasetMetadataJSONSchemaProps,
} from "../../lib/types";

import styles from "./styles";
import * as _schema from "./schema.json";

// Helper to check whether a metadata key is present.
const metadataHasKey = (
  m: PublishedDatasetMetadata,
  k: keyof PublishedDatasetMetadata
) => Object.prototype.hasOwnProperty.call(m, k);

// Instantiate Ajv and add our custom keyword to prevent a compilation error.
const ajv = new Ajv();
ajv.addKeyword("propertiesOrder");

@customElement("arch-dataset-metadata-form")
export class ArchDatasetMetadataForm extends LitElement {
  @property({ type: Object }) metadata: undefined | PublishedDatasetMetadata =
    undefined;

  @query("form") form!: HTMLFormElement;
  @queryAll("input, textarea") inputs!: NodeList;

  static styles = styles;
  static schema = _schema as PublishedDatasetMetadataJSONSchema;
  static validator = ajv.compile(ArchDatasetMetadataForm.schema);
  static propertiesOrder = ArchDatasetMetadataForm.schema
    .propertiesOrder as Array<string>;

  // PublishedDatasetMetadataKeys defines the display order.
  static orderedMetadataKeys = Object.keys(
    ArchDatasetMetadataForm.schema.properties as object
  ).sort((a, b) =>
    ArchDatasetMetadataForm.propertiesOrder.indexOf(a) <
    ArchDatasetMetadataForm.propertiesOrder.indexOf(b)
      ? -1
      : 1
  ) as Array<keyof PublishedDatasetMetadata>;

  private _propToInput(
    name: keyof PublishedDatasetMetadata,
    schema: SomeJSONSchema,
    value: PublishedDatasetMetadataValue,
    valueIndex?: number,
    title?: string,
    showAddButton?: boolean
  ) {
    /* Return the appropriate form input element for the specified args.
     */
    let inputHtml;
    switch (schema.type) {
      case "string":
        if (
          schema.oneOf &&
          Array.isArray(schema.oneOf) &&
          typeof schema.oneOf[0] === "object" &&
          (schema.oneOf[0] as SomeJSONSchema).const
        ) {
          // Display a radio input choice with description.
          inputHtml = html`
            <div>
              ${schema.oneOf.map(
                (choice: {
                  const: string;
                  title?: string;
                  description?: string;
                }) => html`
                  <div class="radio-row">
                    <input
                      type="radio"
                      id=${`${name}-${choice.const}`}
                      name=${name}
                      value=${choice.const}
                      ?checked=${choice.const === value}
                    />
                    <label
                      for=${`${name}-${choice.const}`}
                      value=${choice.const}
                    >
                      ${choice.title ?? choice.const}
                      <br />
                      <em>${choice.description ?? ""}</em>
                    </label>
                  </div>
                `
              )}
            </div>
          `;
        } else if (schema.maxLength <= 100) {
          inputHtml = html`<input
            type="text"
            name=${name}
            value=${value}
            minlength=${schema.minLength}
            maxlength=${schema.maxLength}
            size=${schema.maxLength}
            @change=${(e: Event) =>
              this._updateMetadataValue(
                name,
                (e.target as HTMLInputElement).value,
                valueIndex
              )}
          />`;
        } else {
          inputHtml = html`
            <textarea
              name=${name}
              value=${value}
              minlength=${schema.minLength}
              maxlength=${schema.maxLength}
              cols=${Math.floor(schema.maxLength / 4)}
              @change=${(e: Event) =>
                this._updateMetadataValue(
                  name,
                  (e.target as HTMLTextAreaElement).value,
                  valueIndex
                )}
            >
            </textarea>
          `;
        }
        break;

      default:
        throw new Error(
          `Form input not defined for schema: ${JSON.stringify(schema)}`
        );
        break;
    }

    // Given that we're starting with a fairly limited initial set of metadata fields,
    // I've been asked make all inputs always visible, so am hiding the remove buttons
    // for scalar and non-index-zero array value input rows.
    const hideRemoveButton = valueIndex === undefined || showAddButton;

    return html`
      <div class="input-row">
        ${inputHtml}
        <button
          type="button"
          class="danger remove-value"
          title="Remove Value"
          ?hidden=${hideRemoveButton}
          @click=${() => this._removeMetadataValue(name, valueIndex)}
        >
          &times;
        </button>
        <button
          type="button"
          title="Add another ${title} value"
          ?hidden=${!showAddButton}
          @click=${() => this._addMetadataValue(name)}
        >
          +
        </button>
      </div>
    `;
  }

  render() {
    const { orderedMetadataKeys, schema } = ArchDatasetMetadataForm;
    const { metadata } = this;

    if (metadata === undefined) {
      return html``;
    }

    const availableKeys: Array<keyof PublishedDatasetMetadata> = [];
    const inputs = orderedMetadataKeys.map((k) => {
      // Abort if metadata doesn't specify this key.
      if (!metadataHasKey(metadata, k)) {
        availableKeys.push(k);
        return;
      }

      const value_s = metadata[k] as PublishedDatasetMetadataValue;
      const propSchema = (
        schema.properties as PublishedDatasetMetadataJSONSchemaProps
      )[k];

      // Handle a non-Array type field.
      if (!Array.isArray(value_s)) {
        return html`
          <div class="input-block">
            <label for=${k}>${propSchema.title}</label>
            ${this._propToInput(k, propSchema, value_s)}
          </div>
        `;
      }

      // Handle an Array-type field.
      return html`
        <div class="input-block">
          <label for=${k}>${propSchema.title}</label>
          ${value_s.map((value, i) =>
            this._propToInput(
              k,
              propSchema.items as SomeJSONSchema,
              value,
              i,
              propSchema.title as string,
              i === value_s.length - 1
            )
          )}
        </div>
      `;
    });

    return html`
      <h2>Add/Edit Metadata</h2>
      <form>${inputs}</form>
      ${availableKeys.length === 0
        ? html``
        : html`
            <select name="add-new-field" @input=${this._addFieldSelectHandler}>
              <option value="">Add Metadata Field</option>
              ${availableKeys.map(
                (k) => html`
                  <option value=${k}>
                    ${(
                      schema.properties as PublishedDatasetMetadataJSONSchemaProps
                    )[k].title}
                  </option>
                `
              )}
            </select>
          `}
    `;
  }

  willUpdate(changedProperties: PropertyValues<this>) {
    const { orderedMetadataKeys, schema } = ArchDatasetMetadataForm;
    const { metadata } = this;

    // Normalize the metadata object on any change.
    if (metadata && changedProperties.has("metadata")) {
      // Ensure that scalar-type metadata values are not wrapped in arrays and that each
      // key is represented in order to display all available form fields by default.
      // The ARCH API sends and expects all-array-type metadata values but our schema definition
      // is aware of which fields are scalars vs. arrays, so we normalize to our schema here.
      for (const k of orderedMetadataKeys) {
        // Get the metadata value or a default single-element array that will get unwrapped
        // if necessary.
        const wantsArray =
          (schema.properties as PublishedDatasetMetadataJSONSchemaProps)[k]
            .type === "array";
        const v = metadata[k];
        // If key is undefined, populate it with a default value to ensure that the form
        // field will be displayed.
        if (v === undefined) {
          if (wantsArray) {
            (metadata[k] as Array<string>) = [""];
          } else {
            (metadata[k] as string) = "";
          }
          continue;
        }
        // If the value is an array and the schema wants a scalar, unwrap it if it's
        // a single-element array, otherwise throw an error.
        if (!wantsArray && Array.isArray(v)) {
          if (v.length === 1) {
            // Unwrap the scalar.
            (metadata[k] as string) = v[0];
          } else {
            throw new Error(
              `Invalid non-array type metadata (${k}) value: %{v}`
            );
          }
        }
        // Append an empty string to the end of each array as a new value placeholder.
        if (wantsArray) {
          (metadata[k] as Array<string>) = (
            metadata[k] as Array<string>
          ).concat("");
        }
      }
    }
  }

  updated() {
    /* Ensure that all form input objects have a value property that reflect their
       value attribute.
    */
    (
      Array.from(this.inputs) as Array<HTMLInputElement | HTMLTextAreaElement>
    ).forEach((el) => {
      el.value = el.getAttribute("value") as string;
    });
  }

  private _addFieldSelectHandler(e: Event) {
    const metadata = this.metadata as PublishedDatasetMetadata;
    const { schema } = ArchDatasetMetadataForm;
    const target = e.target as HTMLSelectElement;
    const name = target.value as keyof PublishedDatasetMetadata;
    if (
      (schema.properties as PublishedDatasetMetadataJSONSchemaProps)[name]
        .type === "array"
    ) {
      (metadata[name] as Array<string>) = [""];
    } else {
      (metadata[name] as string) = "";
    }
    // Reselect the first, placeholder option.
    target.options[0].selected = true;
    this.requestUpdate();
  }

  private _addMetadataValue(metadataKey: keyof PublishedDatasetMetadata) {
    const metadata = this.metadata as PublishedDatasetMetadata;
    const values = metadata[metadataKey] as Array<string>;
    values.push("");
    this.requestUpdate();
  }

  private _updateMetadataValue(
    metadataKey: keyof PublishedDatasetMetadata,
    value: string,
    valueIndex?: number
  ) {
    const metadata = this.metadata as PublishedDatasetMetadata;
    if (valueIndex === undefined) {
      (metadata[metadataKey] as string) = value;
    } else {
      (metadata[metadataKey] as Array<string>)[valueIndex] = value;
    }
    this.requestUpdate();
  }

  private _removeMetadataValue(
    metadataKey: keyof PublishedDatasetMetadata,
    valueIndex?: number
  ) {
    const metadata = this.metadata as PublishedDatasetMetadata;
    /* Remove a value, and perhaps a key, from metadata. */
    if (valueIndex === undefined) {
      // This is a scalar / non-Array value, so remove the whole key.
      delete metadata[metadataKey];
    } else {
      // This is an Array-type value.
      const values = metadata[metadataKey] as Array<string>;
      // Remove the specified values index.
      values.splice(valueIndex, 1);
      // If the array is now empty, remove the whole key.
      if (values.length === 0) {
        delete metadata[metadataKey];
      }
    }
    this.requestUpdate();
  }
}

// Injects the tag into the global name space
declare global {
  interface HTMLElementTagNameMap {
    "arch-dataset-metadata-form": ArchDatasetMetadataForm;
  }
}

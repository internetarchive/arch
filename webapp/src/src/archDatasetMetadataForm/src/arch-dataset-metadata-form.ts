import Ajv from "ajv";
import { LitElement, html } from "lit";
import {
  customElement,
  property,
  state,
  query,
  queryAll,
} from "lit/decorators.js";

import {
  PublishedDatasetMetadata,
  PublishedDatasetMetadataJSONSchema,
  PublishedDatasetMetadataKeys,
  PublishedDatasetMetadataValue,
} from "../../lib/types";

import styles from "./styles";
import * as _schema from "./schema.json";

// Helper to check whether a metadata key is present.
const metadataHasKey = (
  m: PublishedDatasetMetadata,
  k: PublishedDatasetMetadataKeys
) => Object.prototype.hasOwnProperty.call(m, k);

@customElement("arch-dataset-metadata-form")
export class ArchDatasetMetadataForm extends LitElement {
  @property({ type: Object }) metadata!: PublishedDatasetMetadata;

  @query("form") form!: HTMLFormElement;
  @queryAll("input, textarea") inputs!: NodeList;

  static styles = styles;
  static schema = _schema as PublishedDatasetMetadataJSONSchema;
  static validator = new Ajv().compile(ArchDatasetMetadataForm.schema);

  // PublishedDatasetMetadataKeys defines the display order.
  static orderedMetadataKeys = Array.from(
    Object.keys(PublishedDatasetMetadataKeys)
  ) as Array<PublishedDatasetMetadataKeys>;

  private _propToInput(
    name: PublishedDatasetMetadataKeys,
    schema: PublishedDatasetMetadataJSONSchema["properties"],
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
          typeof schema.oneOf[0] === "object" &&
          schema.oneOf[0].const
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
        throw new Error(`Form input not defined for schema: ${schema}`);
        break;
    }
    return html`
      <div class="input-row">
        ${inputHtml}
        <button
          type="button"
          class="danger remove-value"
          title="Remove Value"
          @click=${(e: Event) => this._removeMetadataValue(name, valueIndex)}
        >
          &times;
        </button>
        <button
          type="button"
          title="Add another ${title} value"
          style="visibility: ${showAddButton ? "visible" : "hidden"}"
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

    let availableKeys: Array<PublishedDatasetMetadataKeys> = [];
    const inputs = orderedMetadataKeys.map((k) => {
      // Abort if metadata doesn't specify this key.
      if (!metadataHasKey(metadata, k)) {
        availableKeys.push(k);
        return;
      }
      const value_s = metadata[k] as PublishedDatasetMetadataValue;
      const propSchema = schema.properties[k];

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
              propSchema.items,
              value,
              i,
              propSchema.title,
              i === value_s.length - 1
            )
          )}
        </div>
      `;
    });

    return html`
      <form>${inputs}</form>
      ${availableKeys.length === 0
        ? html``
        : html`
            <select name="add-new-field" @input=${this._addFieldSelectHandler}>
              <option value="">Add Metadata Field</option>
              ${availableKeys.map(
                (k) => html`
                  <option value=${k}>${schema.properties[k].title}</option>
                `
              )}
            </select>
          `}
    `;
  }

  willUpdate(changedProperties: Map<PropertyKey, undefined>) {
    const { schema } = ArchDatasetMetadataForm;
    const { metadata } = this;
    // Ensure that scalar-type metadata values are not wrapped in arrays.
    if (changedProperties.has("metadata")) {
      for (const [k, v] of Object.entries(metadata)) {
        if (schema.properties[k].type !== "array" && Array.isArray(v)) {
          if (v.length !== 1) {
            throw new Error(
              `Invalid non-array type metadata (${k}) value: %{v}`
            );
          } else {
            // Unwrap the scalar.
            metadata[k as PublishedDatasetMetadataKeys] = v[0];
          }
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
    const { metadata } = this;
    const { schema } = ArchDatasetMetadataForm;
    const target = e.target as HTMLSelectElement;
    const name = target.value as PublishedDatasetMetadataKeys;
    metadata[name] = schema.properties[name].type === "array" ? [""] : "";
    // Reselect the first, placeholder option.
    target.options[0].selected = true;
    this.requestUpdate();
  }

  private _addMetadataValue(metadataKey: PublishedDatasetMetadataKeys) {
    const { metadata } = this;
    const values = this.metadata[metadataKey] as Array<string>;
    values.push("");
    this.requestUpdate();
  }

  private _updateMetadataValue(
    metadataKey: PublishedDatasetMetadataKeys,
    value: string,
    valueIndex?: number
  ) {
    if (valueIndex === undefined) {
      this.metadata[metadataKey] = value;
    } else {
      (this.metadata[metadataKey] as Array<string>)[valueIndex] = value;
    }
    this.requestUpdate();
  }

  private _removeMetadataValue(
    metadataKey: PublishedDatasetMetadataKeys,
    valueIndex?: number
  ) {
    /* Remove a value, and perhaps a key, from metadata. */
    if (valueIndex === undefined) {
      // This is a scalar / non-Array value, so remove the whole key.
      delete this.metadata[metadataKey];
    } else {
      // This is an Array-type value.
      const values = this.metadata[metadataKey] as Array<string>;
      // Remove the specified values index.
      values.splice(valueIndex, 1);
      // If the array is now empty, remove the whole key.
      if (values.length === 0) {
        delete this.metadata[metadataKey];
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

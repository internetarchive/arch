import { css } from "lit";

import { global } from "../../lib/styles";

export default [
  global,
  css`
    :host {
      display: block;
    }

    label,
    input,
    select {
      font-size: 0.9rem;
    }

    input,
    textarea {
      font-size: 0.9rem;
      border: none;
      background-color: #eee;
      padding: 0.6rem;
    }

    input[type="text"],
    textarea {
      width: 42rem;
      max-width: 42rem;
    }

    textarea {
      height: 4rem;
      vertical-align: top;
    }

    div.input-block {
      margin-bottom: 0.7rem;
    }

    div.input-row {
      display: flex;
      padding-bottom: 0.4rem;
    }

    div.input-row:last-of-type {
      padding-bottom: 0;
    }

    div.radio-row {
      display: flex;
      width: 42rem;
      margin-bottom: 0.5rem;
      background-color: #eee;
      padding: 0.4rem;
    }

    div.radio-row:last-child {
      margin-bottom: 0;
    }

    div.radio-row > input {
      align-self: flex-start;
      cursor: pointer;
    }

    div.radio-row > label {
      flex-grow: 1;
      cursor: pointer;
    }

    div.radio-row > label > em {
      font-weight: normal;
      line-height: 1.1em;
    }

    label {
      display: block;
      font-weight: bold;
      padding-bottom: 0.3rem;
      color: #444;
    }

    div.input-row input,
    div.input-row textarea {
      padding: 0.4rem;
    }

    div.input-row button {
    }

    div.input-row button {
      margin-left: 0.4rem;
    }

    select[name="add-new-field"] {
      margin: 0.5rem 0;
      padding: 0.3rem;
      cursor: pointer;
    }

    button.remove-value {
      margin-left: 0.3rem;
    }
  `,
];

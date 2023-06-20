import { css } from "lit";

import { global } from "../../lib/styles";

export default [
  global,
  css`
    :host {
      display: block;
    }

    h2 {
      margin: 1rem 0;
      color: #444;
    }

    label,
    input,
    select {
      font-size: 0.9rem;
    }

    input,
    textarea {
      font-size: 0.9rem;
      border: solid #aaa 1px;
      background-color: #fff;
      padding: 0.6rem;
      width: 100%;
    }

    textarea {
      height: 4rem;
      vertical-align: top;
      font-family: inherit;
    }

    div.input-block {
      margin-bottom: 1rem;
    }

    div.input-row {
      display: flex;
      padding-bottom: 0.4rem;
      margin-left: 0.5rem;
    }

    div.input-row:last-of-type {
      padding-bottom: 0;
    }

    div.radio-row {
      display: flex;
      background-color: #fff;
      padding: 1rem 0.5rem 1rem 0;
      border: solid #aaa 1px;
      border-bottom: none;
    }

    div.radio-row:last-child {
      border-bottom: solid #aaa 1px;
    }

    div.radio-row > input {
      width: auto;
      margin: 0 1rem;
      cursor: pointer;
    }

    div.radio-row > label {
      flex-grow: 1;
      cursor: pointer;
    }

    div.radio-row > label > em {
      font-weight: normal;
      line-height: 1.3em;
      display: inline-block;
      margin-top: 0.4rem;
      font-family: arial;
    }

    label {
      display: block;
      font-weight: bold;
      padding-bottom: 0.4rem;
      color: #444;
    }

    div.input-row input,
    div.input-row textarea {
      padding: 0.4rem;
    }

    div.input-row button {
      margin-left: 0.4rem;
      border: solid #aaa 1px;
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

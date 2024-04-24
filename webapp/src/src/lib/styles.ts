import { css } from "lit";

export * from "./webservices/src/lib/styles";
import {
  linkColor,
  linkHoverColor,
  defaultButtonBgColor,
  defaultButtonFgColor,
  primaryButtonBgColor,
  primaryButtonFgColor,
  successButtonBgColor,
  successButtonFgColor,
} from "./webservices/src/lib/styles";

/* Bootstrap4 Constants */
const bsPrimaryTextEmphasis = css`#052c65`;
const bsSecondaryTextEmphasis = css`#2b2f32`;
const bsSuccessTextEmphasis = css`#0a3622`;
const bsInfoTextEmphasis = css`#055160`;
const bsWarningTextEmphasis = css`#664d03`;
const bsDangerTextEmphasis = css`#58151c`;
const bsLightTextEmphasis = css`#495057`;
const bsDarkTextEmphasis = css`#495057`;
const bsPrimaryBgSubtle = css`#cfe2ff`;
const bsSecondaryBgSubtle = css`#e2e3e5`;
const bsSuccessBgSubtle = css`#d1e7dd`;
const bsInfoBgSubtle = css`#cff4fc`;
const bsWarningBgSubtle = css`#fff3cd`;
const bsDangerBgSubtle = css`#f8d7da`;
const bsLightBgSubtle = css`#fcfcfd`;
const bsDarkBgSubtle = css`#ced4da`;
const bsPrimaryBorderSubtle = css`#9ec5fe`;
const bsSecondaryBorderSubtle = css`#c4c8cb`;
const bsSuccessBorderSubtle = css`#a3cfbb`;
const bsInfoBorderSubtle = css`#9eeaf9`;
const bsWarningBorderSubtle = css`#ffe69c`;
const bsDangerBorderSubtle = css`#f1aeb5`;
const bsLightBorderSubtle = css`#e9ecef`;
const bsDarkBorderSubtle = css`#adb5bd`;

/**
 * Global Web Component Styles
 */
export const global = css`
  :host {
    color: #222;
    font-family: "Open Sans", Helvetica, Arial, sans-serif;
  }

  a:any-link {
    color: ${linkColor};
    text-decoration: none;
  }

  button {
    white-space: nowrap;
    font-size: 0.9rem;
    border-radius: 3px;
    border: none;
    padding: 0.4rem 1rem;
    cursor: pointer;
    background-color: ${defaultButtonBgColor};
    color: ${defaultButtonFgColor};
  }

  button:disabled {
    cursor: default;
  }

  button.primary {
    background-color: ${primaryButtonBgColor};
    color: ${primaryButtonFgColor};
  }

  button.success {
    background-color: ${successButtonBgColor};
    color: ${successButtonFgColor};
  }

  button.danger {
    background-color: ${bsDangerBgSubtle};
    color: ${bsDangerTextEmphasis};
  }

  a:any-link:hover,
  button.text:hover {
    color: ${linkHoverColor};
    cursor: pointer;
  }

  button.text {
    background: transparent;
    border: none;
    padding: 0;
    color: ${linkColor};
    font-size: 1rem;
  }

  dl {
    margin-block-start: 0;
    margin-block-end: 0;
  }

  dl > div {
    margin-bottom: 1rem;
  }

  dl > div:last-child {
    margin-bottom: 0;
  }

  dt {
    display: inline;
    font-weight: bold;
  }

  dt:after {
    content: ":";
    margin-right: 0.5em;
  }

  dd {
    display: inline;
    margin: 0;
    line-height: 1.2em;
    font-style: italic;
  }

  dd::after {
    content: ",";
    padding-right: 0.2em;
  }

  dd:last-child::after {
    content: none;
    padding-right: 0;
  }

  form > label {
    font-size: 1rem;
    color: black;
    cursor: pointer;
    display: block;
    font-weight: bold;
    line-height: 1.5;
    margin-bottom: 0;
  }

  form > em {
    display: block;
    padding: 0.5rem 0;
    color: #444;
  }

  input,
  select {
    background-color: #fff;
    font-family: inherit;
    border: 1px solid #ccc;
    color: rgba(0, 0, 0, 0.75);
    font-size: 0.875rem;
    padding: 0.5rem;
  }

  label.required:after {
    content: "*";
    color: red;
  }
`;

/**
 * ArchCard "content" Slot <table> Styles
 */
export const cardTable = css`
  table {
    width: 100%;
    border-collapse: collapse;
    table-layout: fixed;
  }

  tr {
    border-bottom: solid #eee 1px;
  }

  tr:last-child {
    border-bottom: none;
  }

  tbody > tr:hover {
    background-color: #f7f7f7;
  }

  th,
  td {
    text-align: left;
    padding: 0.5rem 0.25rem;
  }

  th {
    color: #555;
    font-size: 0.9rem;
  }

  a.view-all {
    font-weight: bold;
  }
`;

export const Bootstrap4Alerts = css`
  .alert {
    position: relative;
    padding: 1rem;
    margin-bottom: 1rem;
    border: 1px solid transparent;
    border-radius: 0.375rem;
  }

  .alert a {
    font-weight: 700;
  }

  .alert-primary {
    color: ${bsPrimaryTextEmphasis};
    background-color: ${bsPrimaryBgSubtle};
    border-color: ${bsPrimaryBorderSubtle};
  }

  .alert-primary a {
    color: ${bsPrimaryTextEmphasis};
  }

  .alert-secondary {
    color: ${bsSecondaryTextEmphasis};
    background-color: ${bsSecondaryBgSubtle};
    border-color: ${bsSecondaryBorderSubtle};
  }

  .alert-secondary a {
    color: ${bsSecondaryTextEmphasis};
  }

  .alert-success {
    color: ${bsSuccessTextEmphasis};
    background-color: ${bsSuccessBgSubtle};
    border-color: ${bsSuccessBorderSubtle};
  }

  .alert-success a {
    color: ${bsSuccessTextEmphasis};
  }

  .alert-info {
    color: ${bsInfoTextEmphasis};
    background-color: ${bsInfoBgSubtle};
    border-color: ${bsInfoBorderSubtle};
  }

  .alert-info a {
    color: ${bsInfoTextEmphasis};
  }

  .alert-warning {
    color: ${bsWarningTextEmphasis};
    background-color: ${bsWarningBgSubtle};
    border-color: ${bsWarningBorderSubtle};
  }

  .alert-warning a {
    color: ${bsWarningTextEmphasis};
  }

  .alert-danger {
    color: ${bsDangerTextEmphasis};
    background-color: ${bsDangerBgSubtle};
    border-color: ${bsDangerBorderSubtle};
  }

  .alert-danger a {
    color: ${bsDangerTextEmphasis};
  }

  .alert-light {
    color: ${bsLightTextEmphasis};
    background-color: ${bsLightBgSubtle};
    border-color: ${bsLightBorderSubtle};
  }

  .alert-light a {
    color: ${bsLightTextEmphasis};
  }

  .alert-dark {
    color: ${bsDarkTextEmphasis};
    background-color: ${bsDarkBgSubtle};
    border-color: ${bsDarkBorderSubtle};
  }

  .alert-dark a {
    color: ${bsDarkTextEmphasis};
  }
`;

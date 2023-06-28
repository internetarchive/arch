import { css } from "lit";

export const linkColor = css`#2b74a1`;
export const linkHoverColor = css`#1b4865`;
export const defaultButtonBgColor = css`#f0f0f0`;
export const defaultButtonFgColor = css`#222`;
export const primaryButtonBgColor = linkColor;
export const primaryButtonFgColor = css`#fff`;
export const primaryButtonHoverBgColor = linkHoverColor;
export const primaryButtonHoverFgColor = primaryButtonFgColor;
export const successButtonBgColor = css`#1e7b34`;
export const successButtonFgColor = css`#fff`;
export const headerBgColor = css`#e3e7e8`;

const Button = {
  Default: {
    backgroundColor: primaryButtonBgColor,
    border: css`none`,
    color: primaryButtonFgColor,
    cursor: css`pointer`,
    hoverBackgroundColor: primaryButtonHoverBgColor,
    hoverColor: primaryButtonHoverFgColor,
    transition: css`background-color 300ms ease-out`,
  },
};

export default css`
  :host {
    /* DataTable action buttons */
    --data-table-action-button-background-color: ${Button.Default
      .backgroundColor};
    --data-table-action-button-border: ${Button.Default.border};
    --data-table-action-button-color: ${Button.Default.color};
    --data-table-action-button-cursor: ${Button.Default.cursor};
    --data-table-action-button-hover-background-color: ${Button.Default
      .hoverBackgroundColor};
    --data-table-action-button-hover-color: ${Button.Default.hoverColor};
    --data-table-action-button-transition: ${Button.Default.transition};

    /* DataTable paginator */
    --data-table-paginator-wrapper-font-size: 1rem;
    --data-table-paginator-control-button-background-color: transparent;
    --data-table-paginator-control-button-border: none;
    --data-table-paginator-control-button-color: #348fc6;
    --data-table-paginator-control-button-padding: 0.25rem;
  }

  a:any-link {
    color: ${linkColor};
  }

  a:hover {
    color: ${linkHoverColor};
  }
`;

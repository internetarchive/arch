/*
 * Paginator Component
 *
 * This component is used to page through results.
 *
 */

import {
  createElement,
  customElementsMaybeDefine,
  html,
  parseElementProps,
} from "../lib/domLib.js";

export default class Paginator extends HTMLElement {
  async connectedCallback() {
    this.props = parseElementProps(this, [
      "numTotal",
      "pageSize",
      "currentPage",
    ]);
    const { numTotal, pageSize, currentPage } = this.props;

    const maxPage = Math.max(Math.ceil(numTotal / pageSize), 1);

    // Define the max number of page number button to display on each side
    // of the current page.
    const MAX_NUM_REACHABLE_PAGES = 1;

    // Determine whether we're going to show the first and last page buttons.
    const showFirstPageButton = currentPage > MAX_NUM_REACHABLE_PAGES + 1;
    const showLastPageButton = maxPage - currentPage > MAX_NUM_REACHABLE_PAGES;

    // Define a helper to calculate the 'start' value for a given page number.
    const pageToStart = (pageNum) => pageSize * (pageNum - 1);

    // Add the previous page button.
    // Ensure that type=button to prevent form submission on click.
    this.appendChild(
      createElement(html`
        <button
          type="button"
          page="${currentPage - 1}"
          start="${pageToStart(currentPage - 1)}"
          ${currentPage === 1 ? "disabled" : ""}
        >
          &lt;
        </button>
      `)
    );

    // Define an Ellipsis element.
    const ellipsisElement = createElement(
      '<span class="position-relative mr-1" style="top: .4em">&#8230;</span>'
    );

    // Maybe add a first page button.
    if (showFirstPageButton) {
      this.appendChild(
        createElement(html`
          <button type="button" page="1" start="${pageToStart(1)}">1</button>
        `)
      );
      this.appendChild(ellipsisElement.cloneNode(true));
    }

    // Add the adjacent page buttons.
    let pageDelta = -MAX_NUM_REACHABLE_PAGES;
    while (pageDelta <= MAX_NUM_REACHABLE_PAGES) {
      const page = currentPage + pageDelta;
      if (
        page > 0 &&
        (page !== 1 || !showFirstPageButton) &&
        (page !== maxPage || !showLastPageButton) &&
        page <= maxPage
      ) {
        const isCurrentPage = page === currentPage;
        this.appendChild(
          createElement(html`
            <button
              type="button"
              page="${page}"
              start="${pageToStart(page)}"
              ${isCurrentPage ? "disabled" : ""}
            >
              ${page}
            </button>
          `)
        );
      }
      pageDelta += 1;
    }

    // Maybe add a last page button.
    if (showLastPageButton) {
      this.appendChild(ellipsisElement.cloneNode(true));
      this.appendChild(
        createElement(html`
          <button
            type="button"
            page="${maxPage}"
            start="${pageToStart(maxPage)}"
          >
            ${maxPage}
          </button>
        `)
      );
    }

    // Add the next page button.
    this.appendChild(
      createElement(html`
        <button
          type="button"
          page="${currentPage + 1}"
          start="${pageToStart(currentPage + 1)}"
          ${currentPage === maxPage ? "disabled" : ""}
        >
          &gt;
        </button>
      `)
    );
  }
}

customElementsMaybeDefine("paginator-control", Paginator);

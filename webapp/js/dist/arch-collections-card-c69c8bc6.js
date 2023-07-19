import{g as t,f as o,i as s,_ as e,b as a,y as i,a as l}from"./chunk-styles-75502ec5.js";import{t as c,a as n}from"./chunk-arch-alert-a83c3a9d.js";import{P as r}from"./chunk-helpers-94bb8932.js";import"./chunk-arch-card-f8a29633.js";import"./chunk-arch-loading-indicator-37c0007d.js";import{h as d}from"./chunk-helpers-139f8162.js";import"./arch-sub-collection-builder-cd409a8e.js";import"./chunk-arch-generate-dataset-form-6a3e9363.js";import"./chunk-query-all-273a2103.js";var h,u=[t,o,s`
    thead > tr.hidden-header {
      color: transparent;
    }

    th.size,
    th.num-datasets {
      text-align: right;
    }

    th.size {
      width: 7rem;
    }

    th.num-datasets {
      width: 10rem;
    }

    td.name {
      text-overflow: ellipsis;
      white-space: nowrap;
      overflow-x: hidden;
    }

    td.size,
    td.num-datasets {
      text-align: right;
    }
  `];let m=h=class extends a{constructor(){super(),this.numTotalCollections=0,this.collections=void 0,this.collectionDatasetCounts=void 0,this.initCollections(),this.initCollectionDatasetCounts()}render(){const{maxDisplayedCollections:t}=h,o=void 0===this.collections,s=this.numTotalCollections>0;return i`
      <arch-card
        title="Collections"
        ctatext=${!o&&s?"Create Custom Collection":""}
        ctahref="${r.buildSubCollection()}"
      >
        <div slot="content">
          <table>
            <thead>
              <tr
                class="${o||!s?"hidden-header":""}"
              >
                <th class="name">Collection Name</th>
                <th class="size">Collection Size</th>
                <th class="num-datasets">Generated Datasets</th>
              </tr>
            </thead>
            <tbody>
              ${(()=>{var e;return o?[i`
              <tr>
                <td colspan="3">
                  <arch-loading-indicator></arch-loading-indicator>
                </td>
              </tr>
            `]:s?(null!==(e=this.collections)&&void 0!==e?e:[]).slice(0,t).map((t=>{var o;return i`
              <tr>
                <td class="name">
                  <a
                    href="/collections/${t.id}"
                    title="${t.name}"
                  >
                    ${t.name}
                  </a>
                </td>
                <td class="size">
                  ${d(-1===t.sortSize?0:t.sortSize,1)}
                </td>
                <td class="num-datasets">
                  ${void 0===this.collectionDatasetCounts?i`<arch-loading-indicator></arch-loading-indicator>`:`${null!==(o=this.collectionDatasetCounts[t.id])&&void 0!==o?o:0} Datasets`}
                </td>
              </tr>
            `})):[i`
              <tr>
                <td colspan="3">
                  <i
                    >No collections found.
                    <a
                      href="https://arch-webservices.zendesk.com/hc/en-us/articles/14795196010772"
                      >Contact us</a
                    >
                    to access collections or report an error.</i
                  >
                </td>
              </tr>
            `]})()}
            </tbody>
          </table>
        </div>
        <div slot="footer">
          ${o||!s?i``:i`
                <a href="/collections" class="view-all">
                  View
                  ${this.numTotalCollections>t?i`All ${this.numTotalCollections}`:i``}
                  Collections
                </a>
              `}
        </div>
      </arch-card>
    `}async initCollections(){const t=await n.collections.get([["sort","=","-lastJobTime"],["limit","=",h.maxDisplayedCollections]]);this.numTotalCollections=t.count,this.collections=t.results}async initCollectionDatasetCounts(){var t;const o=await n.datasets.get([["state","=","Finished"]]),{results:s}=o,e={};for(const o of s){const{collectionId:s}=o;e[s]=(null!==(t=e[s])&&void 0!==t?t:0)+1}this.collectionDatasetCounts=e}};m.maxDisplayedCollections=10,m.styles=u,e([c()],m.prototype,"numTotalCollections",void 0),e([c()],m.prototype,"collections",void 0),e([c()],m.prototype,"collectionDatasetCounts",void 0),m=h=e([l("arch-collections-card")],m);export{m as ArchCollectionsCard};
//# sourceMappingURL=arch-collections-card-c69c8bc6.js.map

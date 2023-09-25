import{g as t,j as o,i as e,_ as s,b as a,y as i,a as l}from"./chunk-styles-48eb3c3c.js";import{t as c}from"./chunk-state-b70c226d.js";import{a as n}from"./chunk-arch-alert-84f9ff63.js";import{P as r}from"./chunk-helpers-7d3afcd3.js";import"./chunk-arch-card-116a2dc2.js";import{a as d}from"./chunk-arch-loading-indicator-93cca752.js";import"./arch-sub-collection-builder-b75d93c9.js";import"./chunk-arch-generate-dataset-form-a4fc78d3.js";import"./chunk-query-all-ac9cba43.js";import"./chunk-scale-large-c2ff54fd.js";var h,u=[t,o,e`
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
  `];let m=h=class extends a{constructor(){super(),this.numTotalCollections=0,this.collections=void 0,this.collectionDatasetCounts=void 0,this.initCollections(),this.initCollectionDatasetCounts()}render(){const{maxDisplayedCollections:t}=h,o=void 0===this.collections,e=this.numTotalCollections>0;return i`
      <arch-card
        title="Collections"
        ctatext=${!o&&e?"Create Custom Collection":""}
        ctahref="${r.buildSubCollection()}"
        ctaTooltipHeader="Custom Collection"
        ctaTooltipText="Combine and filter your collections into a Custom Collection of only the data you need."
        ctaTooltipLearnMoreUrl="https://arch-webservices.zendesk.com/hc/en-us/articles/16107865758228-How-to-create-a-custom-ARCH-collection"
      >
        <div slot="content">
          <table>
            <thead>
              <tr
                class="${o||!e?"hidden-header":""}"
              >
                <th class="name">Collection Name</th>
                <th class="size">Collection Size</th>
                <th class="num-datasets">Generated Datasets</th>
              </tr>
            </thead>
            <tbody>
              ${(()=>{var s;return o?[i`
              <tr>
                <td colspan="3">
                  <arch-loading-indicator></arch-loading-indicator>
                </td>
              </tr>
            `]:e?(null!==(s=this.collections)&&void 0!==s?s:[]).slice(0,t).map((t=>{var o;return i`
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
          ${o||!e?i``:i`
                <a href="/collections" class="view-all">
                  View
                  ${this.numTotalCollections>t?i`All ${this.numTotalCollections}`:i``}
                  Collections
                </a>
              `}
        </div>
      </arch-card>
    `}async initCollections(){const t=await n.collections.get([["sort","=","-lastJobTime"],["limit","=",h.maxDisplayedCollections]]);this.numTotalCollections=t.count,this.collections=t.results}async initCollectionDatasetCounts(){var t;const o=await n.datasets.get([["state","=","Finished"]]),{results:e}=o,s={};for(const o of e){const{collectionId:e}=o;s[e]=(null!==(t=s[e])&&void 0!==t?t:0)+1}this.collectionDatasetCounts=s}};m.maxDisplayedCollections=10,m.styles=u,s([c()],m.prototype,"numTotalCollections",void 0),s([c()],m.prototype,"collections",void 0),s([c()],m.prototype,"collectionDatasetCounts",void 0),m=h=s([l("arch-collections-card")],m);export{m as ArchCollectionsCard};
//# sourceMappingURL=arch-collections-card-3991a3a5.js.map

import{g as t,f as o,i as e,_ as s,b as a,y as i,a as l}from"./chunk-styles-75502ec5.js";import{t as c}from"./chunk-arch-alert-384569c4.js";import{P as n}from"./chunk-helpers-799bde6a.js";import"./chunk-arch-card-f8a29633.js";import"./chunk-arch-loading-indicator-37c0007d.js";import{h as r}from"./chunk-helpers-139f8162.js";import"./arch-sub-collection-builder-5c9f5725.js";import"./chunk-arch-generate-dataset-form-8e4f842f.js";import"./chunk-query-all-273a2103.js";var d,h=[t,o,e`
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
  `];let u=d=class extends a{constructor(){super(),this.collections=void 0,this.collectionDatasetCounts=void 0,this.initCollections(),this.initCollectionDatasetCounts()}render(){var t,o,e;const{numDisplayedCollections:s}=d,a=void 0===this.collections,l=(null!==(t=this.collections)&&void 0!==t?t:[]).length>0;return i`
      <arch-card
        title="Collections"
        ctatext=${!a&&l?"Create Custom Collection":""}
        ctahref="${n.buildSubCollection()}"
      >
        <div slot="content">
          <table>
            <thead>
              <tr
                class="${a||!l?"hidden-header":""}"
              >
                <th class="name">Collection Name</th>
                <th class="size">Collection Size</th>
                <th class="num-datasets">Generated Datasets</th>
              </tr>
            </thead>
            <tbody>
              ${(()=>{var t;return a?[i`
              <tr>
                <td colspan="3">
                  <arch-loading-indicator></arch-loading-indicator>
                </td>
              </tr>
            `]:l?(null!==(t=this.collections)&&void 0!==t?t:[]).slice(0,s).map((t=>{var o;return i`
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
                  ${r(-1===t.sortSize?0:t.sortSize,1)}
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
          ${a||!l?i``:i`
                <a href="/collections" class="view-all">
                  View
                  ${(null!==(o=this.collections)&&void 0!==o?o:[]).length>s?i`All ${(null!==(e=this.collections)&&void 0!==e?e:[]).length}`:i``}
                  Collections
                </a>
              `}
        </div>
      </arch-card>
    `}async initCollections(){this.collections=await(await fetch("/api/collections")).json()}async initCollectionDatasetCounts(){var t;const o=await(await fetch("/api/datasets?state=Finished")).json(),e={};for(const s of o){const{collectionId:o}=s;e[o]=(null!==(t=e[o])&&void 0!==t?t:0)+1}this.collectionDatasetCounts=e}};u.numDisplayedCollections=10,u.styles=h,s([c()],u.prototype,"collections",void 0),s([c()],u.prototype,"collectionDatasetCounts",void 0),u=d=s([l("arch-collections-card")],u);export{u as ArchCollectionsCard};
//# sourceMappingURL=arch-collections-card-0db226ec.js.map

import{g as t,d as a,i as e,_ as s,s as r,y as i,a as d}from"./chunk-styles-4a7b21cd.js";import{t as l}from"./chunk-arch-alert-3c1ceea9.js";import{P as c}from"./chunk-helpers-0f9368e1.js";import{i as o}from"./chunk-helpers-139f8162.js";import"./chunk-arch-card-36181834.js";import"./chunk-arch-loading-indicator-044bc257.js";import"./arch-sub-collection-builder-ff752dbf.js";import"./chunk-arch-generate-dataset-form-94c66d79.js";import"./chunk-query-all-2f0be97e.js";var n,h=[t,a,e`
    thead > tr.hidden-header {
      color: transparent;
    }

    th.date {
      width: 8rem;
      text-align: right;
    }

    td.name,
    td.collection {
      text-overflow: ellipsis;
      white-space: nowrap;
      overflow-x: hidden;
    }

    td.date {
      text-align: right;
    }
  `];let m=n=class extends r{constructor(){super(),this.datasets=void 0,this.initDatasets()}render(){var t,a;const{numDisplayedDatasets:e}=n,s=void 0===this.datasets,r=(null!==(t=this.datasets)&&void 0!==t?t:[]).length>0,d=null!==(a=this.datasets)&&void 0!==a?a:[];return i`
      <arch-card
        title="Recent Datasets"
        ctatext="Generate New Dataset"
        ctahref="/datasets/generate"
      >
        <div slot="content">
          <table>
            <thead>
              <tr class="${s||!r?"hidden-header":""}">
                <th class="name">Dataset</th>
                <th class="collection">Collection Name</th>
                <th class="date">Date Generated</th>
              </tr>
            </thead>
            <tbody>
              ${s?[i`<tr>
              <td colspan="3">
                <arch-loading-indicator></arch-loading-indicator>
              </td>
            </tr>`]:r?d.slice(0,e).map((t=>{const a=`${t.name}${-1!==t.sample?" (Sample)":""}`;return i`
              <tr>
                <td class="name">
                  <a
                    href="${c.dataset(t.id,t.sample)}"
                    title="${a}"
                  >
                    ${a}
                  </a>
                </td>
                <td class="collection" title="${t.collectionName}">
                  ${t.collectionName}
                </td>
                <td class="date">
                  ${o(t.finishedTime)}
                </td>
              </tr>
            `})):[i`<tr>
              <td colspan="3"><i>New datasets will be listed here.</i></td>
            </tr>`]}
            </tbody>
          </table>
        </div>
        <div slot="footer">
          ${s||!r?i``:i`
                <a href="/datasets/explore" class="view-all">
                  View
                  ${d.length>e?i`All ${d.length}`:i``}
                  Datasets
                </a>
              `}
        </div>
      </arch-card>
    `}async initDatasets(){this.datasets=await(await fetch("/api/datasets?state=Finished")).json()}};m.numDisplayedDatasets=10,m.styles=h,s([l()],m.prototype,"datasets",void 0),m=n=s([d("arch-recent-datasets-card")],m);export{m as ArchRecentDatasetsCard};
//# sourceMappingURL=arch-recent-datasets-card-93357e0e.js.map

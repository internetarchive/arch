import{g as t,j as a,i as e,_ as s,b as r,y as i,a as d}from"./chunk-styles-bfd25595.js";import{t as l}from"./chunk-state-97ec0d61.js";import{a as o}from"./chunk-arch-alert-77120ba7.js";import{P as c}from"./chunk-helpers-96c47aea.js";import{i as n}from"./chunk-arch-loading-indicator-418d50f9.js";import"./chunk-arch-card-408fc5f7.js";import"./arch-sub-collection-builder-bd3f0ed5.js";import"./chunk-arch-generate-dataset-form-da670fee.js";import"./chunk-query-all-fe6f60e4.js";import"./chunk-scale-large-1a48103b.js";var h,m=[t,a,e`
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
  `];let p=h=class extends r{constructor(){super(),this.numTotalDatasets=0,this.datasets=void 0,this.initDatasets()}render(){var t,a;const{numTotalDatasets:e}=this,s=void 0===this.datasets,r=(null!==(t=this.datasets)&&void 0!==t?t:[]).length>0,d=null!==(a=this.datasets)&&void 0!==a?a:[];return i`
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
            </tr>`]:r?d.map((t=>{const a=`${t.name}${-1!==t.sample?" (Sample)":""}`;return i`
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
                  ${n(t.finishedTime)}
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
                  ${d.length<e?i`All ${e}`:i``}
                  Datasets
                </a>
              `}
        </div>
      </arch-card>
    `}async initDatasets(){const t=await o.datasets.get([["state","=","Finished"],["sort","=","-startTime"],["limit","=",h.maxDisplayedDatasets]]);this.numTotalDatasets=t.count,this.datasets=t.results}};p.maxDisplayedDatasets=10,p.styles=m,s([l()],p.prototype,"numTotalDatasets",void 0),s([l()],p.prototype,"datasets",void 0),p=h=s([d("arch-recent-datasets-card")],p);export{p as ArchRecentDatasetsCard};
//# sourceMappingURL=arch-recent-datasets-card-157d4378.js.map

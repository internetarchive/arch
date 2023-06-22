import{g as t,f as a,i as e,_ as s,b as r,y as i,a as d}from"./chunk-styles-75502ec5.js";import{t as l,a as o}from"./chunk-arch-alert-a83c3a9d.js";import{P as c}from"./chunk-helpers-94bb8932.js";import{i as n}from"./chunk-helpers-139f8162.js";import"./chunk-arch-card-f8a29633.js";import"./chunk-arch-loading-indicator-37c0007d.js";import"./arch-sub-collection-builder-cd409a8e.js";import"./chunk-arch-generate-dataset-form-6a3e9363.js";import"./chunk-query-all-273a2103.js";var h,m=[t,a,e`
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
//# sourceMappingURL=arch-recent-datasets-card-c1581a70.js.map

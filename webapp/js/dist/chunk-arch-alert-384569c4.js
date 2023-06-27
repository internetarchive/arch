import{e as t,d as e,x as s,g as r,B as i,i as n,_ as a,b as o,y as l,a as d}from"./chunk-styles-75502ec5.js";
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */function h(e){return t({...e,state:!0})}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const c=2;class u{constructor(t){}get _$AU(){return this._$AM._$AU}_$AT(t,e,s){this._$Ct=t,this._$AM=e,this._$Ci=s}_$AS(t,e){return this.update(t,e)}update(t,e){return this.render(...e)}}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */class p extends u{constructor(t){if(super(t),this.it=e,t.type!==c)throw Error(this.constructor.directiveName+"() can only be used in child bindings")}render(t){if(t===e||null==t)return this._t=void 0,this.it=t;if(t===s)return t;if("string"!=typeof t)throw Error(this.constructor.directiveName+"() called with a non-string value");if(t===this.it)return this._t;this.it=t;const r=[t];return r.raw=r,this._t={_$litType$:this.constructor.resultType,strings:r,values:[]}}}p.directiveName="unsafeHTML",p.resultType=1;const g=(t=>(...e)=>({_$litDirective$:t,values:e}))(p);var y,m=[r,i,n`
    div.alert {
      display: flex;
      padding: 0;
    }

    p {
      font-size: 1rem;
      line-height: 1.6rem;
      flex-grow: 1;
      margin: 0;
      padding: 1.2rem 0 1.2rem 1.2rem;
    }

    button {
      align-self: flex-start;
      padding: 1.2rem;
      font-size: 1.2rem;
    }

    button:hover {
      font-weight: bold;
    }
  `];!function(t){t.Danger="danger",t.Dark="dark",t.Info="info",t.Light="light",t.Primary="primary",t.Secondary="secondary",t.Success="success",t.Warning="warning"}(y||(y={}));let f=class extends o{constructor(){super(...arguments),this.alertClass=y.Primary,this.hidden=!1,this.message=""}render(){return l`
      <div
        class="alert alert-${this.alertClass}"
        style="display: ${this.hidden?"none":"flex"}"
        role="alert"
      >
        <p>${g(this.message)}</p>
        <button
          type="button"
          class="close"
          data-dismiss="alert"
          aria-label="Close"
          style="background-color: transparent;"
          @click=${this.hide}
        >
          <span aria-hidden="true">&times;</span>
        </button>
      </div>
    `}hide(){this.setAttribute("hidden","")}show(){this.removeAttribute("hidden")}};f.styles=m,a([t({type:String})],f.prototype,"alertClass",void 0),a([t({type:Boolean})],f.prototype,"hidden",void 0),a([t({type:String})],f.prototype,"message",void 0),f=a([d("arch-alert")],f);export{y as A,h as t};
//# sourceMappingURL=chunk-arch-alert-384569c4.js.map

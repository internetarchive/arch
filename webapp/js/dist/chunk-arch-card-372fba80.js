import{i as t,e as o,b as p,g as e,_ as r,s,y as i,a as m}from"./chunk-styles-d7929693.js";import{t as c}from"./chunk-state-d5912499.js";import{S as v,x as a,A as h,d as n}from"./chunk-scale-large-58ff6c1c.js";const u=t`
:host{--spectrum-overlay-animation-distance:6px;--spectrum-overlay-animation-duration:var(
--spectrum-animation-duration-100
);opacity:0;pointer-events:none;transition:transform var(--spectrum-overlay-animation-duration) ease-in-out,opacity var(--spectrum-overlay-animation-duration) ease-in-out,visibility 0s linear var(--spectrum-overlay-animation-duration);visibility:hidden}:host([open]){opacity:1;pointer-events:auto;transition-delay:0s;visibility:visible}:host([open]) .spectrum-Popover--bottom-end,:host([open]) .spectrum-Popover--bottom-left,:host([open]) .spectrum-Popover--bottom-right,:host([open]) .spectrum-Popover--bottom-start,:host([placement*=bottom][open]){--spectrum-overlay-animation-distance:6px;transform:translateY(var(--spectrum-overlay-animation-distance))}:host([open]) .spectrum-Popover--top-end,:host([open]) .spectrum-Popover--top-left,:host([open]) .spectrum-Popover--top-right,:host([open]) .spectrum-Popover--top-start,:host([placement*=top][open]){--spectrum-overlay-animation-distance:6px;transform:translateY(calc(var(--spectrum-overlay-animation-distance)*-1))}:host([dir=rtl][open]) .spectrum-Popover--start,:host([dir=rtl][open]) .spectrum-Popover--start-bottom,:host([dir=rtl][open]) .spectrum-Popover--start-top,:host([open]) .spectrum-Popover--end,:host([open]) .spectrum-Popover--end-bottom,:host([open]) .spectrum-Popover--end-top,:host([open]) .spectrum-Popover--right-bottom,:host([open]) .spectrum-Popover--right-top,:host([placement*=right][open]){--spectrum-overlay-animation-distance:6px;transform:translateX(var(--spectrum-overlay-animation-distance))}:host([dir=rtl][open]) .spectrum-Popover--end,:host([dir=rtl][open]) .spectrum-Popover--end-bottom,:host([dir=rtl][open]) .spectrum-Popover--end-top,:host([open]) .spectrum-Popover--left-bottom,:host([open]) .spectrum-Popover--left-top,:host([open]) .spectrum-Popover--start,:host([open]) .spectrum-Popover--start-bottom,:host([open]) .spectrum-Popover--start-top,:host([placement*=left][open]){--spectrum-overlay-animation-distance:6px;transform:translateX(calc(var(--spectrum-overlay-animation-distance)*-1))}:host{--spectrum-popover-cross-offset:0;--spectrum-popover-background-color:var(
--spectrum-background-layer-2-color
);--spectrum-popover-border-color:var(--spectrum-gray-400);--spectrum-popover-content-area-spacing-vertical:var(
--spectrum-popover-top-to-content-area
);--spectrum-popover-shadow-horizontal:var(--spectrum-drop-shadow-x);--spectrum-popover-shadow-vertical:var(--spectrum-drop-shadow-y);--spectrum-popover-shadow-blur:var(--spectrum-drop-shadow-blur);--spectrum-popover-shadow-color:var(--spectrum-drop-shadow-color);--spectrum-popover-corner-radius:var(--spectrum-corner-radius-100);--spectrum-popover-pointer-width:var(--spectrum-popover-tip-width);--spectrum-popover-pointer-height:var(--spectrum-popover-tip-height);--spectrum-popover-pointer-edge-offset:calc(var(--spectrum-corner-radius-100) + var(--spectrum-popover-tip-width)/2);--spectrum-popover-pointer-edge-spacing:calc(var(--spectrum-popover-pointer-edge-offset) - var(--spectrum-popover-tip-width)/2)}@media (forced-colors:active){:host{--highcontrast-popover-border-color:CanvasText}}:host{--spectrum-popover-filter:drop-shadow(var(
--mod-popover-shadow-horizontal,var(--spectrum-popover-shadow-horizontal)
) var(
--mod-popover-shadow-vertical,var(--spectrum-popover-shadow-vertical)
) var(--mod-popover-shadow-blur,var(--spectrum-popover-shadow-blur)) var(
--mod-popover-shadow-color,var(--spectrum-popover-shadow-color)
));background-color:var(
--mod-popover-background-color,var(--spectrum-popover-background-color)
);border-color:var(
--highcontrast-popover-border-color,var(--mod-popover-border-color,var(--spectrum-popover-border-color))
);border-radius:var(
--mod-popover-corner-radius,var(--spectrum-popover-corner-radius)
);border-style:solid;border-width:var(
--mod-popover-border-width,var(--spectrum-popover-border-width)
);box-sizing:border-box;display:inline-flex;filter:var(--mod-popover-filter,var(--spectrum-popover-filter));flex-direction:column;outline:none;padding:var(
--mod-popover-content-area-spacing-vertical,var(--spectrum-popover-content-area-spacing-vertical)
) 0;position:absolute}:host([tip]) #tip .triangle{stroke-linecap:square;stroke-linejoin:miter;fill:var(
--highcontrast-popover-background-color,var(
--mod-popover-background-color,var(--spectrum-popover-background-color)
)
);stroke:var(
--highcontrast-popover-border-color,var(--mod-popover-border-color,var(--spectrum-popover-border-color))
);stroke-width:var(
--mod-popover-border-width,var(--spectrum-popover-border-width)
)}*{--mod-popover-filter:none}.spectrum-Popover--top-end,.spectrum-Popover--top-left,.spectrum-Popover--top-right,.spectrum-Popover--top-start,:host([placement*=top]){margin-bottom:var(--spectrum-popover-cross-offset)}.spectrum-Popover--bottom-end,.spectrum-Popover--bottom-left,.spectrum-Popover--bottom-right,.spectrum-Popover--bottom-start,:host([placement*=bottom]){margin-top:var(--spectrum-popover-cross-offset)}.spectrum-Popover--right-bottom,.spectrum-Popover--right-top,:host([placement*=right]){margin-left:var(--spectrum-popover-cross-offset)}.spectrum-Popover--left-bottom,.spectrum-Popover--left-top,:host([placement*=left]){margin-right:var(--spectrum-popover-cross-offset)}.spectrum-Popover--start,.spectrum-Popover--start-bottom,.spectrum-Popover--start-top{margin-inline-end:var(--spectrum-popover-cross-offset)}.spectrum-Popover--end,.spectrum-Popover--end-bottom,.spectrum-Popover--end-top{margin-inline-start:var(--spectrum-popover-cross-offset)}:host([tip]) #tip,:host([tip]) .spectrum-Popover--bottom-end #tip,:host([tip]) .spectrum-Popover--bottom-left #tip,:host([tip]) .spectrum-Popover--bottom-right #tip,:host([tip]) .spectrum-Popover--bottom-start #tip,:host([tip]) .spectrum-Popover--top-end #tip,:host([tip]) .spectrum-Popover--top-left #tip,:host([tip]) .spectrum-Popover--top-right #tip,:host([tip]) .spectrum-Popover--top-start #tip,:host([tip][placement*=bottom]) #tip,:host([tip][placement*=top]) #tip{height:var(
--mod-popover-pointer-height,var(--spectrum-popover-pointer-height)
);left:0;margin:auto;position:absolute;right:0;top:100%;transform:translate(0);width:var(
--mod-popover-pointer-width,var(--spectrum-popover-pointer-width)
)}:host([tip]) .spectrum-Popover--top-left #tip{left:var(
--mod-popover-pointer-edge-spacing,var(--spectrum-popover-pointer-edge-spacing)
);right:auto}:host([tip]) .spectrum-Popover--top-right #tip{left:auto;right:var(
--mod-popover-pointer-edge-spacing,var(--spectrum-popover-pointer-edge-spacing)
)}:host([tip]) .spectrum-Popover--top-start #tip{margin-inline-start:var(
--mod-popover-pointer-edge-spacing,var(--spectrum-popover-pointer-edge-spacing)
)}:host([tip]) .spectrum-Popover--top-end #tip{margin-inline-end:var(
--mod-popover-pointer-edge-spacing,var(--spectrum-popover-pointer-edge-spacing)
)}:host([tip]) .spectrum-Popover--bottom-end #tip,:host([tip]) .spectrum-Popover--bottom-left #tip,:host([tip]) .spectrum-Popover--bottom-right #tip,:host([tip]) .spectrum-Popover--bottom-start #tip,:host([tip][placement*=bottom]) #tip{bottom:100%;top:auto;transform:scaleY(-1)}:host([tip]) .spectrum-Popover--bottom-left #tip{left:var(
--mod-popover-pointer-edge-spacing,var(--spectrum-popover-pointer-edge-spacing)
);right:auto}:host([tip]) .spectrum-Popover--bottom-right #tip{left:auto;right:var(
--mod-popover-pointer-edge-spacing,var(--spectrum-popover-pointer-edge-spacing)
)}:host([tip]) .spectrum-Popover--bottom-start #tip{margin-inline-start:var(
--mod-popover-pointer-edge-spacing,var(--spectrum-popover-pointer-edge-spacing)
)}:host([tip]) .spectrum-Popover--bottom-end #tip{margin-inline-end:var(
--mod-popover-pointer-edge-spacing,var(--spectrum-popover-pointer-edge-spacing)
)}:host([tip]) .spectrum-Popover--end #tip,:host([tip]) .spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--start #tip,:host([tip]) .spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--start-top #tip,:host([tip][placement*=left]) #tip,:host([tip][placement*=right]) #tip{bottom:0;height:var(
--mod-popover-pointer-width,var(--spectrum-popover-pointer-width)
);top:0;width:var(
--mod-popover-pointer-height,var(--spectrum-popover-pointer-height)
)}:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--left-top #tip,:host([tip][placement*=left]) .spectrum-Popover--end #tip,:host([tip][placement*=left]) .spectrum-Popover--end-bottom #tip,:host([tip][placement*=left]) .spectrum-Popover--end-top #tip,:host([tip][placement*=left]) .spectrum-Popover--left-bottom #tip,:host([tip][placement*=left]) .spectrum-Popover--left-top #tip,:host([tip][placement*=left]) .spectrum-Popover--right-bottom #tip,:host([tip][placement*=left]) .spectrum-Popover--right-top #tip,:host([tip][placement*=left]) .spectrum-Popover--start #tip,:host([tip][placement*=left]) .spectrum-Popover--start-bottom #tip,:host([tip][placement*=left]) .spectrum-Popover--start-top #tip,:host([tip][placement*=left][placement*=left]) #tip,:host([tip][placement*=right]) .spectrum-Popover--left-bottom #tip,:host([tip][placement*=right]) .spectrum-Popover--left-top #tip,:host([tip][placement*=right][placement*=left]) #tip{left:100%;right:auto}:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--right-top #tip,:host([tip][placement*=left]) .spectrum-Popover--right-bottom #tip,:host([tip][placement*=left]) .spectrum-Popover--right-top #tip,:host([tip][placement*=left][placement*=right]) #tip,:host([tip][placement*=right]) .spectrum-Popover--end #tip,:host([tip][placement*=right]) .spectrum-Popover--end-bottom #tip,:host([tip][placement*=right]) .spectrum-Popover--end-top #tip,:host([tip][placement*=right]) .spectrum-Popover--left-bottom #tip,:host([tip][placement*=right]) .spectrum-Popover--left-top #tip,:host([tip][placement*=right]) .spectrum-Popover--right-bottom #tip,:host([tip][placement*=right]) .spectrum-Popover--right-top #tip,:host([tip][placement*=right]) .spectrum-Popover--start #tip,:host([tip][placement*=right]) .spectrum-Popover--start-bottom #tip,:host([tip][placement*=right]) .spectrum-Popover--start-top #tip,:host([tip][placement*=right][placement*=right]) #tip{left:auto;right:100%;transform:scaleX(-1)}:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--start-top #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--start-top #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--start-top #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--start-top #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--start-top #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--start-top #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--start-top #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--start-top #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--start-top #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--end-top #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--left-top #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--right-top #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--start-top #tip,:host([tip][placement*=left]) .spectrum-Popover--end-top #tip,:host([tip][placement*=left]) .spectrum-Popover--left-top #tip,:host([tip][placement*=left]) .spectrum-Popover--right-top #tip,:host([tip][placement*=left]) .spectrum-Popover--start-top #tip,:host([tip][placement*=right]) .spectrum-Popover--end-top #tip,:host([tip][placement*=right]) .spectrum-Popover--left-top #tip,:host([tip][placement*=right]) .spectrum-Popover--right-top #tip,:host([tip][placement*=right]) .spectrum-Popover--start-top #tip{bottom:auto;top:var(
--mod-popover-pointer-edge-spacing,var(--spectrum-popover-pointer-edge-spacing)
)}:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--end-bottom.spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--end-top.spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--end.spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--left-bottom.spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--left-top.spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--right-bottom.spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--right-top.spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--start-bottom.spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--start-top.spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--left-bottom #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--right-bottom #tip,:host([tip]) .spectrum-Popover--start.spectrum-Popover--start-bottom #tip,:host([tip][placement*=left]) .spectrum-Popover--end-bottom #tip,:host([tip][placement*=left]) .spectrum-Popover--left-bottom #tip,:host([tip][placement*=left]) .spectrum-Popover--right-bottom #tip,:host([tip][placement*=left]) .spectrum-Popover--start-bottom #tip,:host([tip][placement*=right]) .spectrum-Popover--end-bottom #tip,:host([tip][placement*=right]) .spectrum-Popover--left-bottom #tip,:host([tip][placement*=right]) .spectrum-Popover--right-bottom #tip,:host([tip][placement*=right]) .spectrum-Popover--start-bottom #tip{bottom:var(
--mod-popover-pointer-edge-spacing,var(--spectrum-popover-pointer-edge-spacing)
);top:auto}:host([tip]) .spectrum-Popover--start #tip,:host([tip]) .spectrum-Popover--start-bottom #tip,:host([tip]) .spectrum-Popover--start-top #tip{margin-inline-start:100%}:host([dir=rtl][tip]) .spectrum-Popover--start #tip,:host([dir=rtl][tip]) .spectrum-Popover--start-bottom #tip,:host([dir=rtl][tip]) .spectrum-Popover--start-top #tip{transform:none}:host([tip]) .spectrum-Popover--end #tip,:host([tip]) .spectrum-Popover--end-bottom #tip,:host([tip]) .spectrum-Popover--end-top #tip{margin-inline-end:100%;transform:scaleX(-1)}:host([dir=rtl][tip]) .spectrum-Popover--end #tip,:host([dir=rtl][tip]) .spectrum-Popover--end-bottom #tip,:host([dir=rtl][tip]) .spectrum-Popover--end-top #tip{transform:scaleX(1)}:host{--spectrum-popover-border-width:var(
--system-spectrum-popover-border-width
)}:host{--sp-popover-tip-size:24px;--mod-popover-pointer-width:max(var(--spectrum-popover-pointer-width),var(--spectrum-popover-pointer-height));--mod-popover-pointer-height:max(var(--spectrum-popover-pointer-width),var(--spectrum-popover-pointer-height));clip-path:none;max-height:100%;max-width:100%;min-width:min-content}::slotted(*){overscroll-behavior:contain}:host([placement*=left]) #tip[style],:host([placement*=right]) #tip[style]{bottom:auto}:host([placement*=bottom]) #tip[style],:host([placement*=top]) #tip[style]{right:auto}.block{display:block;height:50%;width:100%}.inline{display:block;height:100%;width:50%}:host([placement*=left]) .block,:host([placement*=right]) .block{display:none}:host([placement*=bottom]) .inline,:host([placement*=top]) .inline{display:none}::slotted(.visually-hidden){clip:rect(0,0,0,0);border:0;clip-path:inset(50%);height:1px;margin:0 -1px -1px 0;overflow:hidden;padding:0;position:absolute;white-space:nowrap;width:1px}::slotted(sp-menu){margin:0}:host([dialog]){min-width:var(
--mod-popover-dialog-min-width,var(--spectrum-popover-dialog-min-width,270px)
);padding:var(
--mod-popover-dialog-padding,var(--spectrum-popover-dialog-padding,30px 29px)
)}
`;var l=Object.defineProperty,d=Object.getOwnPropertyDescriptor,P=(t,o,p,e)=>{for(var r,s=e>1?void 0:e?d(o,p):o,i=t.length-1;i>=0;i--)(r=t[i])&&(s=(e?r(o,p,s):r(s))||s);return e&&s&&l(o,p,s),s};class g extends v{constructor(){super(...arguments),this.dialog=!1,this.open=!1,this.tip=!1}static get styles(){return[u]}renderTip(){return a`
            <div id="tip" aria-hidden="true">
                <svg class="tip block" viewBox="0 -0.5 16 9">
                    <path class="triangle" d="M-1,-1 8,8 17,-1"></path>
                </svg>
                <svg class="tip inline" viewBox="0 -0.5 9 16">
                    <path class="triangle" d="M-1,-1 8,8 -1,17"></path>
                </svg>
            </div>
        `}update(t){super.update(t)}render(){return a`
            <slot></slot>
            ${this.tip?this.renderTip():h}
        `}}P([o({type:Boolean,reflect:!0})],g.prototype,"dialog",2),P([o({type:Boolean,reflect:!0})],g.prototype,"open",2),P([o({reflect:!0})],g.prototype,"placement",2),P([o({type:Boolean,reflect:!0})],g.prototype,"tip",2),P([p("#tip")],g.prototype,"tipElement",2),n("sp-popover",g);var b=[e,t`
    sp-popover {
      padding: 1rem;
    }

    div[role="dialog"] > h2 {
      font-size: 1rem;
      border-bottom: solid 1px #888;
      padding-bottom: 0.5rem;
      margin-top: 0;
    }

    div[role="dialog"] > h2 > span {
      display: inline-block;
      width: 1.4rem;
    }

    div[role="dialog"] > div.text {
      margin-left: 1.4rem;
      font-size: 0.9rem;
      line-height: 1.2em;
      color: #444;
    }

    div[role="dialog"] > div.learn-more {
      text-align: right;
      padding-top: 1rem;
    }
  `];let f=class extends s{constructor(){super(...arguments),this.header=void 0,this.text=void 0,this.learnMoreUrl=void 0,this.width=12,this.isOpen=!1}render(){const{header:t,text:o,learnMoreUrl:p,width:e}=this;return i`
      <sp-theme color="light" scale="medium">
        <overlay-trigger
          placement="bottom-end"
          type="inline"
          @sp-opened=${()=>this.isOpen=!0}
          @sp-closed=${()=>this.isOpen=!1}
        >
          <button
            class="text"
            slot="trigger"
            title="Learn about ${this.header}"
          >
            ${this.isOpen?i`<strong>&#9432;</strong>`:i`&#9432;`}
          </button>
          <sp-popover slot="click-content" position="bottom" tip open>
            <div role="dialog">
              ${t?i` <h2><span>&#9432;</span>${t}</h2>`:i``}
              <div class="text" style="width: ${e}rem">${o||""}</div>
              ${p?i`
                    <div class="learn-more">
                      <a href="${p}" target="_blank">Learn More</a>
                    </div>
                  `:i``}
            </div>
          </sp-popover>
        </overlay-trigger>
      </sp-theme>
    `}};f.styles=b,r([o({type:String})],f.prototype,"header",void 0),r([o({type:String})],f.prototype,"text",void 0),r([o({type:String})],f.prototype,"learnMoreUrl",void 0),r([o({type:Number})],f.prototype,"width",void 0),r([c()],f.prototype,"isOpen",void 0),f=r([m("arch-tooltip")],f);var y=[e,t`
    :host {
      display: block;
      background-color: #fff;
      padding: 1rem 1rem 3rem 1rem;
      height: calc(100% - 4rem);
      box-shadow: 1px 1px 6px #888;
      font-size: 0.95rem;
      border-radius: 6px;
      position: relative;
    }

    :host .header {
      display: flex;
    }

    :host .header > *:first-child {
      flex-grow: 1;
    }

    :host .header > a {
      margin: auto;
    }

    :host .footer {
      position: absolute;
      left: 0;
      right: 0;
      bottom: 1rem;
      text-align: center;
    }

    :host arch-tooltip {
      margin: auto 0 auto 0.5rem;
    }
  `];let w=class extends s{constructor(){super(...arguments),this.title="Title",this.headerLevel=2,this.ctaText=void 0,this.ctaHref=void 0,this.ctaTooltipHeader=void 0,this.ctaTooltipText=void 0,this.ctaTooltipLearnMoreUrl=void 0}get header(){switch(this.headerLevel){case 1:return i`<h1>${this.title}</h1>`;case 2:return i`<h2>${this.title}</h2>`;case 3:return i`<h3>${this.title}</h3>`;case 4:return i`<h4>${this.title}</h4>`;case 5:return i`<h5>${this.title}</h5>`;default:return i`<h6>${this.title}</h6>`}}render(){const{ctaTooltipHeader:t,ctaTooltipText:o,ctaTooltipLearnMoreUrl:p}=this;return i`
      <section>
        <div class="header">
          ${this.header}
          ${this.ctaText&&this.ctaHref?i`
                <a href="${this.ctaHref}">${this.ctaText}</a>
                ${t||o||p?i`
                      <arch-tooltip
                        .header=${t}
                        .text=${o}
                        .learnMoreUrl=${p}
                      ></arch-tooltip>
                    `:i``}
              `:""}
        </div>
        <hr />
        <slot name="content"></slot>
        <div class="footer">
          <slot name="footer"></slot>
        </div>
      </section>
    `}};w.styles=y,r([o({type:String})],w.prototype,"title",void 0),r([o({type:Number})],w.prototype,"headerLevel",void 0),r([o({type:String})],w.prototype,"ctaText",void 0),r([o({type:String})],w.prototype,"ctaHref",void 0),r([o({type:String})],w.prototype,"ctaTooltipHeader",void 0),r([o({type:String})],w.prototype,"ctaTooltipText",void 0),r([o({type:String})],w.prototype,"ctaTooltipLearnMoreUrl",void 0),w=r([m("arch-card")],w);
//# sourceMappingURL=chunk-arch-card-372fba80.js.map

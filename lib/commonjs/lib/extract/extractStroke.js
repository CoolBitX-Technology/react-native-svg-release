"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = extractStroke;
var _extractBrush = _interopRequireDefault(require("./extractBrush"));
var _extractOpacity = _interopRequireDefault(require("./extractOpacity"));
var _extractLengthList = _interopRequireDefault(require("./extractLengthList"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
const caps = {
  butt: 0,
  square: 2,
  round: 1
};
const joins = {
  miter: 0,
  bevel: 2,
  round: 1
};
const vectorEffects = {
  none: 0,
  default: 0,
  nonScalingStroke: 1,
  'non-scaling-stroke': 1,
  inherit: 2,
  uri: 3
};
function extractStroke(o, props, inherited) {
  const {
    stroke,
    strokeOpacity,
    strokeLinecap,
    strokeLinejoin,
    strokeDasharray,
    strokeWidth,
    strokeDashoffset,
    strokeMiterlimit,
    vectorEffect
  } = props;
  if (stroke != null) {
    inherited.push('stroke');
    o.stroke = (0, _extractBrush.default)(stroke);
  }
  if (strokeWidth != null) {
    inherited.push('strokeWidth');
    o.strokeWidth = strokeWidth;
  }
  if (strokeOpacity != null) {
    inherited.push('strokeOpacity');
    o.strokeOpacity = (0, _extractOpacity.default)(strokeOpacity);
  }
  if (strokeDasharray != null) {
    inherited.push('strokeDasharray');
    const strokeDash = !strokeDasharray || strokeDasharray === 'none' ? null : (0, _extractLengthList.default)(strokeDasharray);
    o.strokeDasharray = strokeDash && strokeDash.length % 2 === 1 ? strokeDash.concat(strokeDash) : strokeDash;
  }
  if (strokeDashoffset != null) {
    inherited.push('strokeDashoffset');
    o.strokeDashoffset = strokeDasharray && strokeDashoffset ? +strokeDashoffset || 0 : null;
  }
  if (strokeLinecap != null) {
    inherited.push('strokeLinecap');
    o.strokeLinecap = strokeLinecap && caps[strokeLinecap] || 0;
  }
  if (strokeLinejoin != null) {
    inherited.push('strokeLinejoin');
    o.strokeLinejoin = strokeLinejoin && joins[strokeLinejoin] || 0;
  }
  if (strokeMiterlimit != null) {
    inherited.push('strokeMiterlimit');
    o.strokeMiterlimit = (strokeMiterlimit && typeof strokeMiterlimit !== 'number' ? parseFloat(strokeMiterlimit) : strokeMiterlimit) || 4;
  }
  if (vectorEffect != null) {
    o.vectorEffect = vectorEffect && vectorEffects[vectorEffect] || 0;
  }
}
//# sourceMappingURL=extractStroke.js.map
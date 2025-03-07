import extractBrush from './extractBrush';
import extractOpacity from './extractOpacity';
import { processColor } from 'react-native';
const fillRules = {
  evenodd: 0,
  nonzero: 1
};
const defaultFill = {
  type: 0,
  payload: processColor('black')
};
export default function extractFill(o, props, inherited) {
  const {
    fill,
    fillRule,
    fillOpacity
  } = props;
  if (fill != null) {
    inherited.push('fill');
    o.fill = !fill && typeof fill !== 'number' ? defaultFill : extractBrush(fill);
  } else {
    // we want the default value of fill to be black to match the spec
    o.fill = defaultFill;
  }
  if (fillOpacity != null) {
    inherited.push('fillOpacity');
    o.fillOpacity = extractOpacity(fillOpacity);
  }
  if (fillRule != null) {
    inherited.push('fillRule');
    o.fillRule = fillRule && fillRules[fillRule] === 0 ? 0 : 1;
  }
}
//# sourceMappingURL=extractFill.js.map
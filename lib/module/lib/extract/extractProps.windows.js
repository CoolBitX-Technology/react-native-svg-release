import extractFill from './extractFill';
import extractStroke from './extractStroke';
import extractTransform from './extractTransform';
import extractResponder from './extractResponder';
import extractOpacity from './extractOpacity';
import { idPattern } from '../util';
const clipRules = {
  evenodd: 0,
  nonzero: 1
};
export function propsAndStyles(props) {
  const {
    style
  } = props;
  return !style ? props : {
    ...(Array.isArray(style) ? Object.assign({}, ...style) : style),
    ...props
  };
}
function getMarker(marker) {
  if (!marker) {
    return undefined;
  }
  const matched = marker.match(idPattern);
  return matched ? matched[1] : undefined;
}
export default function extractProps(props, ref) {
  const {
    id,
    opacity,
    onLayout,
    clipPath,
    clipRule,
    display,
    mask,
    marker,
    markerStart = marker,
    markerMid = marker,
    markerEnd = marker
  } = props;
  const extracted = {};
  const inherited = [];
  extractResponder(extracted, props, ref);
  extractFill(extracted, props, inherited);
  extractStroke(extracted, props, inherited);
  if (props.color) {
    extracted.color = props.color;
  }
  if (inherited.length) {
    extracted.propList = inherited;
  }
  const matrix = extractTransform(props);
  if (matrix !== null) {
    extracted.matrix = matrix;
  }
  if (opacity != null) {
    extracted.opacity = extractOpacity(opacity);
  }
  if (display != null) {
    extracted.display = display === 'none' ? 'none' : undefined;
  }
  if (onLayout) {
    extracted.onLayout = onLayout;
  }
  if (markerStart) {
    extracted.markerStart = getMarker(markerStart);
  }
  if (markerMid) {
    extracted.markerMid = getMarker(markerMid);
  }
  if (markerEnd) {
    extracted.markerEnd = getMarker(markerEnd);
  }
  if (id) {
    extracted.name = String(id);
  }
  if (clipRule) {
    extracted.clipRule = clipRules[clipRule] === 0 ? 0 : 1;
  }
  if (clipPath) {
    const matched = clipPath.match(idPattern);
    if (matched) {
      extracted.clipPath = matched[1];
    } else {
      console.warn('Invalid `clipPath` prop, expected a clipPath like "#id", but got: "' + clipPath + '"');
    }
  }
  if (mask) {
    const matched = mask.match(idPattern);
    if (matched) {
      extracted.mask = matched[1];
    } else {
      console.warn('Invalid `mask` prop, expected a mask like "#id", but got: "' + mask + '"');
    }
  }
  return extracted;
}
export function extract(instance, props) {
  return extractProps(propsAndStyles(props), instance);
}
export function withoutXY(instance, props) {
  return extractProps({
    ...propsAndStyles(props),
    x: null,
    y: null
  }, instance);
}
//# sourceMappingURL=extractProps.windows.js.map
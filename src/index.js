import React, { Component } from 'react';
import {
  requireNativeComponent,
  Platform,
  processColor,
  View,
  Text,
} from 'react-native';
import PropTypes from 'prop-types';

const messageContainerStyle = {
  height: 80,
  alignItems: 'center',
  justifyContent: 'center',
  paddingTop: 20,
  backgroundColor: '#1C1E2A',
  marginHorizontal: 10,
  borderRadius: 8,
};

const LINKING_ERROR =
  "The package 'react-native-gesture-password-android' doesn't seem to be linked. Make sure you have linked it properly.";

const ComponentName = 'PatternLockerView';

let NativePatternLocker = null;

if (Platform.OS === 'android') {
  try {
    NativePatternLocker = requireNativeComponent(ComponentName);
  } catch (e) {
    console.warn(LINKING_ERROR);
  }
}

class GesturePassword extends Component {
  static propTypes = {
    style: PropTypes.object,
    textStyle: PropTypes.object,
    message: PropTypes.string,
    normalColor: PropTypes.string,
    rightColor: PropTypes.string,
    wrongColor: PropTypes.string,
    status: PropTypes.oneOf(['normal', 'right', 'wrong']),
    interval: PropTypes.number,
    allowCross: PropTypes.bool,
    innerCircle: PropTypes.bool,
    outerCircle: PropTypes.bool,
    transparentLine: PropTypes.bool,
    onStart: PropTypes.func,
    onEnd: PropTypes.func,
    onReset: PropTypes.func,
  };

  static defaultProps = {
    message: '',
    normalColor: '#5FA8FC',
    rightColor: '#5FA8FC',
    wrongColor: '#D93609',
    status: 'normal',
    interval: 0,
    allowCross: false,
    innerCircle: true,
    outerCircle: true,
  };

  renderNativeAndroid() {
    const { onStart, onEnd, onReset, normalColor, rightColor, wrongColor, message, style, textStyle, ...rest } = this.props;

    const currentColor =
      this.props.status === 'wrong'
        ? this.props.wrongColor
        : this.props.rightColor || '#5FA8FC';

    const nativeProps = {
      ...rest,
      style: [{ flex: 1 }, style],
      normalColor: normalColor ? processColor(normalColor) : undefined,
      rightColor: rightColor ? processColor(rightColor) : undefined,
      wrongColor: wrongColor ? processColor(wrongColor) : undefined,
      onStartEvent: () => {
        onStart && onStart();
      },
      onEndEvent: (event) => {
        onEnd && onEnd(event.nativeEvent.password);
      },
      onResetEvent: () => {
        onReset && onReset();
      },
    };

    return (
      <View style={[{ flex: 1 }, style]}>
        {message ? (
          <View style={messageContainerStyle}>
            <Text style={[{ color: currentColor, fontSize: 14 }, textStyle]}>
              {message}
            </Text>
          </View>
        ) : null}
        <NativePatternLocker
          {...nativeProps}
          style={{ flex: 1 }}
        />
        {this.props.children}
      </View>
    );
  }

  render() {
    if (Platform.OS !== 'android' || !NativePatternLocker) {
      const color =
        this.props.status === 'wrong'
          ? this.props.wrongColor
          : this.props.rightColor;
      return (
        <View style={[{ flex: 1, backgroundColor: '#292B38', alignItems: 'center', justifyContent: 'center' }, this.props.style]}>
          <Text style={[{ color: color || '#5FA8FC', fontSize: 14 }, this.props.textStyle]}>
            {this.props.message || 'Pattern Lock is Android only'}
          </Text>
        </View>
      );
    }
    return this.renderNativeAndroid();
  }
}

export default GesturePassword;

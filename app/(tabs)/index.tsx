import { Image, StyleSheet, Platform } from 'react-native';
import React, { useState } from 'react';
import { HelloWave } from '@/components/HelloWave';
import ParallaxScrollView from '@/components/ParallaxScrollView';
import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';

import {NativeModules, Button, requireNativeComponent, View} from 'react-native';
const {CameraApiModule, LivePreviewModule} = NativeModules;
const MyNativeView = requireNativeComponent('CameraView');

import { launchImageLibrary } from 'react-native-image-picker';

export default function HomeScreen() {

  const [selectedImage, setSelectedImage] = useState(null);

  const onPress = () => {
      CameraApiModule.createCalendarEvent('testName', 'testLocation');
      LivePreviewModule.startLivePreviewActivity(selectedImage);
  };

  const openImagePicker = () => {
      const options = {
        mediaType: 'photo',
        includeBase64: false,
        maxHeight: 2000,
        maxWidth: 2000,
      };

      launchImageLibrary(options, (response) => {
        if (response.didCancel) {
          console.log('User cancelled image picker');
        } else if (response.error) {
          console.log('Image picker error: ', response.error);
        } else {
          let imageUri = response.uri || response.assets?.[0]?.uri;
          const parts = imageUri.toString().split('/');
          const url = parts[parts.length - 1];
          console.log(">>AAAB", url)
          setSelectedImage(url);
        }
      });
    };

  return (
    <ParallaxScrollView headerBackgroundColor={{ light: '#A1CEDC', dark: '#1D3D47' }} >
        <Button title="Click to invoke your native module!" color="#841584" onPress={onPress} />
        <Button title="Image select" color="#841584" onPress={openImagePicker} />
    </ParallaxScrollView>
  );
}

const styles = StyleSheet.create({
  titleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  stepContainer: {
    gap: 8,
    marginBottom: 8,
  },
  reactLogo: {
    height: 178,
    width: 290,
    bottom: 0,
    left: 0,
    position: 'absolute',
  },
  container: {
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
  },
  nativeView: {
      width: 300,
      height: 400,
  },
});

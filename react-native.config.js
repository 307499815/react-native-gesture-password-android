module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import com.reactnativeandroipatternlocker.PatternLockerPackage;',
        packageInstance: 'new PatternLockerPackage()',
      },
      ios: {} // explicitly empty for autolinking compatibility
    },
  },
};

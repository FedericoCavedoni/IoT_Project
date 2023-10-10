static void parseAndConvert(const uint8_t* data, size_t dataSize, char* outputString, size_t outputSize) {
    size_t i;
    for (i = 0; i < dataSize && i < outputSize - 1; i++) {
        outputString[i] = (char)data[i];
    }
    outputString[i] = '\0'; 
}
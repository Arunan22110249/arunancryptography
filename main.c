#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <stdlib.h>

#define MAX_TEXT_LENGTH 1024

// 1. Caesar Cipher
void caesarCipher(const char* text, int shift, int mode) {
    char result[MAX_TEXT_LENGTH];
    for (int i = 0; text[i] != '\0'; i++) {
        char ch = text[i];
        if (isalpha(ch)) {
            char base = isupper(ch) ? 'A' : 'a';
            ch = ((ch - base + (mode ? shift : 26 - shift)) % 26) + base;
        }
        result[i] = ch;
    }
    result[strlen(text)] = '\0';
    printf("%s text: %s\n", mode ? "Encrypted" : "Decrypted", result);
}

// 2. Atbash Cipher
void atbashCipher(const char* text) {
    char result[MAX_TEXT_LENGTH];
    for (int i = 0; text[i] != '\0'; i++) {
        char ch = text[i];
        if (isupper(ch)) ch = 'Z' - (ch - 'A');
        else if (islower(ch)) ch = 'z' - (ch - 'a');
        result[i] = ch;
    }
    result[strlen(text)] = '\0';
    printf("Transformed text: %s\n", result);
}

// 3. August Cipher (shift = 8, fixed)
void augustCipher(const char* text, int mode) {
    char result[MAX_TEXT_LENGTH];
    int shift = 8;
    for (int i = 0; text[i] != '\0'; i++) {
        char ch = text[i];
        if (isalpha(ch)) {
            char base = isupper(ch) ? 'A' : 'a';
            ch = ((ch - base + (mode ? shift : 26 - shift)) % 26) + base;
        }
        result[i] = ch;
    }
    result[strlen(text)] = '\0';
    printf("%s text: %s\n", mode ? "Encrypted" : "Decrypted", result);
}

// 4. Affine Cipher
int modInverse(int a, int m) {
    a = a % m;
    for (int x = 1; x < m; x++) if ((a * x) % m == 1) return x;
    return -1;
}
void affineCipher(const char* text, int a, int b, int mode) {
    char result[MAX_TEXT_LENGTH];
    int a_inv = modInverse(a, 26);
    for (int i = 0; text[i] != '\0'; i++) {
        char ch = text[i];
        if (isalpha(ch)) {
            char base = isupper(ch) ? 'A' : 'a';
            if (mode)
                ch = ((a * (ch - base) + b) % 26) + base;
            else
                ch = (a_inv * ((ch - base - b + 26)) % 26) + base;
        }
        result[i] = ch;
    }
    result[strlen(text)] = '\0';
    printf("%s text: %s\n", mode ? "Encrypted" : "Decrypted", result);
}

// 5. Vigenère Cipher
void vigenereCipher(const char* text, const char* key, int mode) {
    char result[MAX_TEXT_LENGTH];
    int keyLen = strlen(key);
    for (int i = 0, j = 0; text[i] != '\0'; i++) {
        char ch = text[i];
        if (isalpha(ch)) {
            char base = isupper(ch) ? 'A' : 'a';
            int k = toupper(key[j % keyLen]) - 'A';
            if (mode)
                ch = ((ch - base + k) % 26) + base;
            else
                ch = ((ch - base - k + 26) % 26) + base;
            j++;
        }
        result[i] = ch;
    }
    result[strlen(text)] = '\0';
    printf("%s text: %s\n", mode ? "Encrypted" : "Decrypted", result);
}

// 6. Gronsfeld Cipher
void gronsfeldCipher(const char* text, const char* key, int mode) {
    char result[MAX_TEXT_LENGTH];
    int keyLen = strlen(key);
    for (int i = 0, j = 0; text[i] != '\0'; i++) {
        char ch = text[i];
        if (isalpha(ch)) {
            char base = isupper(ch) ? 'A' : 'a';
            int k = key[j % keyLen] - '0';
            if (mode)
                ch = ((ch - base + k) % 26) + base;
            else
                ch = ((ch - base - k + 26) % 26) + base;
            j++;
        }
        result[i] = ch;
    }
    result[strlen(text)] = '\0';
    printf("%s text: %s\n", mode ? "Encrypted" : "Decrypted", result);
}

// 7. Beaufort Cipher
void beaufortCipher(const char* text, const char* key) {
    char result[MAX_TEXT_LENGTH];
    int keyLen = strlen(key);
    for (int i = 0, j = 0; text[i] != '\0'; i++) {
        char ch = text[i];
        if (isalpha(ch)) {
            char base = isupper(ch) ? 'A' : 'a';
            int p = ch - base;
            int k = toupper(key[j % keyLen]) - 'A';
            ch = ((k - p + 26) % 26) + base;
            j++;
        }
        result[i] = ch;
    }
    result[strlen(text)] = '\0';
    printf("Encrypted text: %s\n", result);
}

// 8. Autokey Cipher
void autokeyCipher(const char* text, const char* key, int mode) {
    char result[MAX_TEXT_LENGTH];
    char fullKey[MAX_TEXT_LENGTH];
    strcpy(fullKey, key);
    if (mode) strcat(fullKey, text);

    for (int i = 0, j = 0; text[i] != '\0'; i++) {
        char ch = text[i];
        if (isalpha(ch)) {
            char base = isupper(ch) ? 'A' : 'a';
            int k = toupper(fullKey[j]) - 'A';
            if (mode)
                ch = ((ch - base + k) % 26) + base;
            else
                ch = ((ch - base - k + 26) % 26) + base;
            j++;
        }
        result[i] = ch;
    }
    result[strlen(text)] = '\0';
    printf("%s text: %s\n", mode ? "Encrypted" : "Decrypted", result);
}

// 9. N-Gram Cipher (bigrams)
void ngramCipher(const char* text) {
    char paddedText[MAX_TEXT_LENGTH];
    strcpy(paddedText, text);
    if (strlen(paddedText) % 2 != 0) strcat(paddedText, "X");

    for (int i = 0; i < strlen(paddedText); i += 2) {
        char temp = paddedText[i];
        paddedText[i] = paddedText[i + 1];
        paddedText[i + 1] = temp;
    }
    printf("Encrypted text: %s\n", paddedText);
}

// 10. Hill Cipher (2x2 matrix)
void hillCipher(const char* text) {
    int key[2][2] = {{3, 3}, {2, 5}};
    int len = strlen(text);
    if (len % 2 != 0) len++;

    char padded[MAX_TEXT_LENGTH];
    strcpy(padded, text);
    if (strlen(text) % 2 != 0) strcat(padded, "X");

    printf("Encrypted text: ");
    for (int i = 0; i < len; i += 2) {
        int a = toupper(padded[i]) - 'A';
        int b = toupper(padded[i+1]) - 'A';
        int c1 = (key[0][0] * a + key[0][1] * b) % 26;
        int c2 = (key[1][0] * a + key[1][1] * b) % 26;
        printf("%c%c", c1 + 'A', c2 + 'A');
    }
    printf("\n");
}

// 11. Rail Fence Cipher
void railFenceCipher(const char* text, int rails) {
    int len = strlen(text);
    char rail[rails][len];
    for (int i = 0; i < rails; i++) for (int j = 0; j < len; j++) rail[i][j] = '\0';

    int row = 0, dir = 1;
    for (int i = 0; i < len; i++) {
        rail[row][i] = text[i];
        row += dir;
        if (row == rails - 1 || row == 0) dir *= -1;
    }

    printf("Encrypted text: ");
    for (int i = 0; i < rails; i++)
        for (int j = 0; j < len; j++)
            if (rail[i][j] != '\0') putchar(rail[i][j]);
    printf("\n");
}

// 12. Route Cipher
void routeCipher(const char* text) {
    int len = strlen(text);
    int size = 1;
    while (size * size < len) size++;
    char grid[size][size];
    for (int i = 0, k = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            if (k < len) grid[i][j] = text[k++];
            else grid[i][j] = 'X';
        }
    }

    printf("Encrypted text: ");
    for (int j = 0; j < size; j++)
        for (int i = 0; i < size; i++)
            printf("%c", grid[i][j]);
    printf("\n");
}

// 13. Myszkowski Cipher
void myszkowskiCipher(const char* text, const char* key) {
    int len = strlen(text);
    int klen = strlen(key);
    int rows = (len + klen - 1) / klen;
    char matrix[rows][klen];

    int k = 0;
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < klen; j++) {
            if (k < len) matrix[i][j] = text[k++];
            else matrix[i][j] = 'X';
        }
    }

    printf("Encrypted text: ");
    for (char c = '1'; c <= '9'; c++) {
        for (int j = 0; j < klen; j++) {
            if (key[j] == c) {
                for (int i = 0; i < rows; i++) {
                    printf("%c", matrix[i][j]);
                }
            }
        }
    }
    printf("\n");
}

#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <stdlib.h>

#define MAX_TEXT_LENGTH 1024

// (All cipher function definitions stay the same here...)
// [Truncated to save space in this explanation — retain all cipher functions above]

int main() {
    int choice, shift, a, b, rails;
    char text[MAX_TEXT_LENGTH], key[MAX_TEXT_LENGTH];

    printf("\nClassical Ciphers Menu\n");
    printf("1. Caesar Cipher\n2. Atbash Cipher\n3. August Cipher\n4. Affine Cipher\n5. Vigenere Cipher\n6. Gronsfeld Cipher\n");
    printf("7. Beaufort Cipher\n8. Autokey Cipher\n9. N-Gram Cipher\n10. Hill Cipher\n11. Rail Fence Cipher\n");
    printf("12. Route Cipher\n13. Myszkowski Cipher\nEnter your choice: ");
    scanf("%d", &choice);
    getchar(); // to consume newline

    printf("Enter text: ");
    fgets(text, MAX_TEXT_LENGTH, stdin);
    text[strcspn(text, "\n")] = '\0';

    switch (choice) {
        case 1:
            printf("Enter shift: "); scanf("%d", &shift);
            caesarCipher(text, shift, 1);
            break;
        case 2:
            atbashCipher(text);
            break;
        case 3:
            augustCipher(text, 1);
            break;
        case 4:
            printf("Enter a and b (e.g. 5 8): "); scanf("%d %d", &a, &b);
            affineCipher(text, a, b, 1);
            break;
        case 5:
            printf("Enter key: "); scanf("%s", key);
            vigenereCipher(text, key, 1);
            break;
        case 6:
            printf("Enter numeric key: "); scanf("%s", key);
            gronsfeldCipher(text, key, 1);
            break;
        case 7:
            printf("Enter key: "); scanf("%s", key);
            beaufortCipher(text, key);
            break;
        case 8:
            printf("Enter key: "); scanf("%s", key);
            autokeyCipher(text, key, 1);
            break;
        case 9:
            ngramCipher(text);
            break;
        case 10:
            hillCipher(text);
            break;
        case 11:
            printf("Enter number of rails: "); scanf("%d", &rails);
            railFenceCipher(text, rails);
            break;
        case 12:
            routeCipher(text);
            break;
        case 13:
            printf("Enter key (numbers, e.g., 31422): "); scanf("%s", key);
            myszkowskiCipher(text, key);
            break;
        default:
            printf("Invalid choice!\n");
    }

    return 0;
}


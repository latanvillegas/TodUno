#!/data/data/com.termux/files/usr/bin/bash
# Configure a new, persistent TodUno Android signing key and GitHub Actions secrets.
# Run on the developer's Termux device only.
set -Eeuo pipefail
umask 077
export LC_ALL=C
export LANG=C

readonly REPO="latanvillegas/TodUno"
readonly KEY_DIR="${HOME}/.config/toduno/signing"
readonly KEYSTORE="${KEY_DIR}/toduno-release.jks"
readonly KEY_ALIAS="upload"

for cmd in gh keytool base64 sha256sum awk; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        printf 'Missing %s. Install prerequisites: pkg install openjdk-21 gh coreutils\n' "$cmd" >&2
        exit 1
    fi
done

if ! gh auth status --hostname github.com >/dev/null 2>&1; then
    printf 'GitHub authentication is required.\n'
    gh auth login --hostname github.com --git-protocol https --web
fi
gh repo view "$REPO" --json name >/dev/null

mkdir -p "$KEY_DIR"
chmod 700 "$KEY_DIR"

printf '\nPrivate key path: %s\n' "$KEYSTORE"
printf 'Enter a strong password and store it separately in a password manager.\n'
IFS= read -r -s -p 'Signing password (at least 16 characters): ' signing_password
printf '\n'
if (( ${#signing_password} < 16 )); then
    printf 'Signing password must have at least 16 characters.\n' >&2
    exit 1
fi
IFS= read -r -s -p 'Repeat signing password: ' confirm_password
printf '\n'
if [[ "$signing_password" != "$confirm_password" ]]; then
    printf 'Passwords do not match; no key was generated.\n' >&2
    exit 1
fi
unset confirm_password
export TODUNO_SIGNING_PASSWORD="$signing_password"
unset signing_password
trap 'unset TODUNO_SIGNING_PASSWORD' EXIT

if [[ -e "$KEYSTORE" ]]; then
    printf 'Existing keystore detected; reusing it without generating a new key.\n'
    keytool -list -keystore "$KEYSTORE" -alias "$KEY_ALIAS" \
        -storepass:env TODUNO_SIGNING_PASSWORD >/dev/null
else
    keytool -genkeypair -noprompt \
        -keystore "$KEYSTORE" \
        -storetype JKS \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 4096 \
        -sigalg SHA256withRSA \
        -validity 10000 \
        -dname "CN=TodUno, OU=Mobile, O=TodUno, C=PE" \
        -storepass:env TODUNO_SIGNING_PASSWORD \
        -keypass:env TODUNO_SIGNING_PASSWORD
    chmod 600 "$KEYSTORE"
fi

# The default keytool export is a DER-encoded X.509 certificate.
# Its SHA-256 matches apksigner's certificate SHA-256 digest.
CERT_SHA256="$(
    keytool -exportcert -keystore "$KEYSTORE" -alias "$KEY_ALIAS" \
        -storepass:env TODUNO_SIGNING_PASSWORD |
        sha256sum | awk '{print $1}'
)"
if [[ ! "$CERT_SHA256" =~ ^[0-9a-f]{64}$ ]]; then
    printf 'Could not compute certificate digest; secrets were not uploaded.\n' >&2
    exit 1
fi

printf '\nPublic signing certificate SHA-256:\n%s\n' "$CERT_SHA256"
printf '\nUploading encrypted secrets to %s...\n' "$REPO"

base64 -w 0 "$KEYSTORE" | gh secret set KEYSTORE_BASE64 -R "$REPO" --app actions
printf '%s' "$TODUNO_SIGNING_PASSWORD" | gh secret set STORE_PASSWORD -R "$REPO" --app actions
printf '%s' "$TODUNO_SIGNING_PASSWORD" | gh secret set KEY_PASSWORD -R "$REPO" --app actions
printf '%s' "$CERT_SHA256" | gh secret set RELEASE_CERT_SHA256 -R "$REPO" --app actions

printf '\nConfigured Actions secret names (values are not shown):\n'
gh secret list -R "$REPO" --app actions

printf '\nSUCCESS: Keep a secure offline backup of the JKS and its password.\n'
printf 'Never share the keystore, password, or its base64 representation.\n'
printf 'Do not create tag v2.0.0 until the new pipeline and device tests pass.\n'

const GSI_SCRIPT_SRC = 'https://accounts.google.com/gsi/client'

let scriptLoadPromise: Promise<void> | null = null

function loadGsiScript(): Promise<void> {
  if (window.google?.accounts.id) return Promise.resolve()

  if (!scriptLoadPromise) {
    scriptLoadPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = GSI_SCRIPT_SRC
      script.async = true
      script.defer = true
      script.onload = () => resolve()
      script.onerror = () => reject(new Error('Google Identity Services の読み込みに失敗しました'))
      document.head.appendChild(script)
    })
  }
  return scriptLoadPromise
}

/**
 * Google Identity Services（AUTH-01 のログインボタン）を扱う composable。
 *
 * スクリプトの読み込みは初回呼び出し時のみ行われ、以降は使い回される。
 */
export function useGoogleIdentity() {
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string

  /**
   * 指定した要素に Google ログインボタンを描画する。
   * ユーザーがログインすると `onCredential` に ID トークンが渡される。
   */
  async function renderButton(
    container: HTMLElement,
    onCredential: (idToken: string) => void,
  ): Promise<void> {
    await loadGsiScript()
    const accountsId = window.google?.accounts.id
    if (!accountsId) {
      throw new Error('Google Identity Services の初期化に失敗しました')
    }

    accountsId.initialize({
      client_id: clientId,
      callback: (response) => onCredential(response.credential),
    })
    accountsId.renderButton(container, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      shape: 'pill',
    })
  }

  return { renderButton }
}

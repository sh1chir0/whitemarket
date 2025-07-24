const mainBlock = document.getElementById('main-block')

export function settings() {
    const formContainer = document.createElement('div')
    formContainer.className = 'form-container'

    const input1 = document.createElement('input');
    input1.className = 'form-input'
    input1.type = 'text'
    input1.id = 'public-key'
    input1.placeholder = 'Public key...'

    const input2 = document.createElement('input')
    input2.className = 'form-input'
    input2.type = 'text'
    input2.id = 'secret-key'
    input2.placeholder = 'Secret key...'

    const button = document.createElement('button')
    button.className = 'svg-button'
    button.id = 'save-btn'
    button.innerHTML = `
      <svg width="175" height="50" viewBox="0 0 175 50" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="175" height="50" rx="6" fill="url(#paint0_linear_29_199)"/>
        <text x="55" y="32" fill="white" font-size="16" font-family="sans-serif">ЗБЕРЕГТИ</text>
        <path d="M40.875 39.125H18.125C17.263 39.125 16.4364 38.7826 15.8269 38.1731C15.2174 37.5636 14.875 36.737 14.875 35.875V13.125C14.875 12.263 15.2174 11.4364 15.8269 10.8269C16.4364 10.2174 17.263 9.875 18.125 9.875H36L44.125 18V35.875C44.125 36.737 43.7826 37.5636 43.1731 38.1731C42.5636 38.7826 41.737 39.125 40.875 39.125Z" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M37.625 39.125V26.125H21.375V39.125" stroke="white" stroke-width="2" stroke-linejoin="round"/>
        <path d="M21.375 9.875V18H34.375" stroke="white" stroke-width="2" stroke-linejoin="round"/>
        <defs>
          <linearGradient id="paint0_linear_29_199" x1="42.0343" y1="28.3133" x2="168.096" y2="25.8709" gradientUnits="userSpaceOnUse">
            <stop stop-color="#74CBBA"/>
            <stop offset="1" stop-color="#8DD294"/>
          </linearGradient>
        </defs>
      </svg>
    `

    formContainer.appendChild(input1)
    formContainer.appendChild(input2)
    formContainer.appendChild(button)

    mainBlock.appendChild(formContainer)

    const secondFormContainer = document.createElement('div')
    secondFormContainer.className = 'form-container'
    secondFormContainer.style.marginTop = '30px'

    const singleInput = document.createElement('input')
    singleInput.className = 'form-input'
    singleInput.type = 'text'
    singleInput.id = 'api-token'
    singleInput.placeholder = 'New password...'

    const secondButton = document.createElement('button');
    secondButton.className = 'svg-button'
    secondButton.id = 'save-token-btn'
    secondButton.innerHTML = `
      <svg width="175" height="50" viewBox="0 0 175 50" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="175" height="50" rx="6" fill="url(#paint0_linear_token)"/>
        <text x="55" y="32" fill="white" font-size="16" font-family="sans-serif">ЗБЕРЕГТИ</text>
        <path d="M40.875 39.125H18.125C17.263 39.125 16.4364 38.7826 15.8269 38.1731C15.2174 37.5636 14.875 36.737 14.875 35.875V13.125C14.875 12.263 15.2174 11.4364 15.8269 10.8269C16.4364 10.2174 17.263 9.875 18.125 9.875H36L44.125 18V35.875C44.125 36.737 43.7826 37.5636 43.1731 38.1731C42.5636 38.7826 41.737 39.125 40.875 39.125Z" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M37.625 39.125V26.125H21.375V39.125" stroke="white" stroke-width="2" stroke-linejoin="round"/>
        <path d="M21.375 9.875V18H34.375" stroke="white" stroke-width="2" stroke-linejoin="round"/>
        <defs>
          <linearGradient id="paint0_linear_token" x1="42.0343" y1="28.3133" x2="168.096" y2="25.8709" gradientUnits="userSpaceOnUse">
            <stop stop-color="#74CBBA"/>
            <stop offset="1" stop-color="#8DD294"/>
          </linearGradient>
        </defs>
      </svg>
    `

    secondFormContainer.appendChild(singleInput)
    secondFormContainer.appendChild(secondButton)

    mainBlock.appendChild(secondFormContainer)

    const thirdFormContainer = document.createElement('div')
    thirdFormContainer.className = 'form-container'
    thirdFormContainer.style.marginTop = '30px'

    const offerDelayInput = document.createElement('input');
    offerDelayInput.className = 'form-input'
    offerDelayInput.type = 'number'
    offerDelayInput.id = 'offer-delay'
    offerDelayInput.placeholder = 'Offer delay...'

    const targetDelayInput = document.createElement('input')
    targetDelayInput.className = 'form-input'
    targetDelayInput.type = 'number'
    targetDelayInput.id = 'target-delay'
    targetDelayInput.placeholder = 'Target delay...'

    const delayButton = document.createElement('button');
    delayButton.className = 'svg-button'
    delayButton.id = 'delay-save-btn'
    delayButton.innerHTML = `
      <svg width="175" height="50" viewBox="0 0 175 50" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="175" height="50" rx="6" fill="url(#paint0_linear_token)"/>
        <text x="55" y="32" fill="white" font-size="16" font-family="sans-serif">ЗБЕРЕГТИ</text>
        <path d="M40.875 39.125H18.125C17.263 39.125 16.4364 38.7826 15.8269 38.1731C15.2174 37.5636 14.875 36.737 14.875 35.875V13.125C14.875 12.263 15.2174 11.4364 15.8269 10.8269C16.4364 10.2174 17.263 9.875 18.125 9.875H36L44.125 18V35.875C44.125 36.737 43.7826 37.5636 43.1731 38.1731C42.5636 38.7826 41.737 39.125 40.875 39.125Z" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M37.625 39.125V26.125H21.375V39.125" stroke="white" stroke-width="2" stroke-linejoin="round"/>
        <path d="M21.375 9.875V18H34.375" stroke="white" stroke-width="2" stroke-linejoin="round"/>
        <defs>
          <linearGradient id="paint0_linear_token" x1="42.0343" y1="28.3133" x2="168.096" y2="25.8709" gradientUnits="userSpaceOnUse">
            <stop stop-color="#74CBBA"/>
            <stop offset="1" stop-color="#8DD294"/>
          </linearGradient>
        </defs>
      </svg>
    `
    thirdFormContainer.appendChild(offerDelayInput)
    thirdFormContainer.appendChild(targetDelayInput)
    thirdFormContainer.appendChild(delayButton)

    mainBlock.appendChild(thirdFormContainer)

    document.getElementById('delay-save-btn').addEventListener('click', () => {
        const offerDelay = document.getElementById('offer-delay').value
        const targetDelay = document.getElementById('target-delay').value

        fetch('/api/config/change-delays', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                offerDelay: offerDelay,
                targetDelay: targetDelay
            })
        }).then(response => {
            if (response.ok) {
                alert('Delays updated successfully');
            } else {
                alert('Error updating delays');
            }
        })
    })

    document.getElementById('save-btn').addEventListener('click', () => {
        const publicKey = document.getElementById('public-key').value
        const secretKey = document.getElementById('secret-key').value

        fetch('/api/config/change-keys', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                publicKey: publicKey,
                secretKey: secretKey
            })
        }).then(response => {
            if (response.ok) {
                alert('Keys updated successfully');
            } else {
                alert('Error updating keys');
            }
        })
    })

    document.getElementById('save-token-btn').addEventListener('click', () => {
        const publicKey = document.getElementById('api-token').value
        fetch('/api/config/change-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/text'
            },
            body: publicKey
        }).then(response => {
            if (response.ok) {
                alert('Password updated successfully');
                location.reload();
            } else {
                alert('Error...');
            }
        })
    })
}
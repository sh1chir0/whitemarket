const mainBlock = document.getElementById('main-block')

export function targets() {
    const signal = controllerTargets.signal

    fetch('/api/targets/get-all', {
        method: 'GET',
        signal: signal
    }).then(response => response.json())
        .then(data => {
            const table = document.createElement('div')

            table.className = 'table-eight'
            table.innerHTML = `
                    <div></div>
                    <div class="back-header"><div class="header">НАЗВА</div></div>
                    <div class="back-header"><div class="header">МІН.ПОРІГ</div></div>
                    <div class="back-header"><div class="header">МАКС.ПОРІГ</div></div>
                    <div class="back-header"><div class="header">ЦІНА</div></div>
                    <div class="back-header"><div class="header">МАКС. ТАРГЕТ</div></div>
                    <div class="back-header"><div class="header">DM MIN <img src="/img/lock.png" alt="lock" class="lock-icon"></div></div>
                    <div class="back-header"><div class="header">DM MIN <img src="/img/green lock.png" alt="lock" class="lock-icon"></div></div>
                    <div class="back-header"><div class="header">ВИДАЛИТИ</div></div>`

            mainBlock.appendChild(table)

            data.forEach(item => {
                table.insertAdjacentHTML('beforeend', `
                        <div class="cell"><img src="${item.imageLink}" alt="lock" class="skin-img" data-asset="${item.assetId}" data-type="image"></div>
                        <div class="cell"><input style="text-align: left;" type="text" value="${item.name}" readonly data-asset="${item.id}" data-type="name"></div>
                        <div class="cell"><input type="number" value="${item.minPrice}" data-asset="${item.id}" data-type="minPrice"></div>
                        <div class="cell"><input type="number" value="${item.maxPrice}" data-asset="${item.id}" data-type="maxPrice"></div>
                        <div class="cell"><input type="number" value="${item.price}" readonly data-asset="${item.id}" data-type="price"></div>
                        <div class="cell"><input type="text" value="${item.maxTarget}" readonly data-asset="${item.id}" data-type="maxTarget"></div>
                        <div class="cell"><input type="text" value="${item.minWithLock}" readonly data-asset="${item.id}" data-type="locked"></div>
                        <div class="cell"><input type="text" value="${item.minWithoutLock}" readonly data-asset="${item.id}" data-type="unlocked"></div>
                        <div class="cell"><button class="delete-btn" data-asset="${item.id}">🗑</button></div>
                    `)
            })

            mainBlock.appendChild(table)

            const searchInput = document.getElementById('searchInput')

            searchInput.addEventListener('input', () => {
                const filter = searchInput.value.toLowerCase()
                const nameInputs = table.querySelectorAll('input[data-type="name"]')

                nameInputs.forEach(input => {
                    const nameValue = input.value.toLowerCase()
                    const nameCell = input.closest('.cell')
                    const rowStartIndex = Array.from(table.children).indexOf(nameCell)
                    const cellsInRow = Array.from(table.children).slice(rowStartIndex - 1, rowStartIndex + 8)

                    if (nameValue.includes(filter)) {
                        cellsInRow.forEach(cell => cell.style.display = '')
                    } else {
                        cellsInRow.forEach(cell => cell.style.display = 'none')
                    }
                })
            })

            document.querySelectorAll('.delete-btn').forEach(button => {
                button.addEventListener('click', (e) => {
                    const id = e.target.dataset.asset;

                    fetch(`/api/targets/delete/${id}`, {
                        method: 'GET',
                    })
                        .then(async response => {
                            if (!response.ok) {
                                throw new Error('Помилка при відправці даних')
                            }

                            mainBlock.innerHTML = ``
                            targets()
                            return response.text()
                        })
                        .then(data => {
                            console.log(data)
                        })
                        .catch(error => {
                            console.error('Помилка:', error)
                        })
                });
            });

            document.getElementById('save-btn').addEventListener('click', () => {
                const inputs = document.querySelectorAll('input[data-asset]')
                const results = {}

                inputs.forEach(input => {
                    const id = input.dataset.asset
                    const type = input.dataset.type
                    const value = input.value

                    if (!results[id]) {
                        results[id] = {id}
                    }

                    switch (type) {
                        case "maxPrice":
                            results[id].maxPrice = value
                            break;
                        case "minPrice":
                            results[id].minPrice = value
                            break;
                    }
                })

                const resultArray = Object.values(results)
                console.log(resultArray)
                fetch('/api/targets/update', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(resultArray)
                })
                    .then(async response => {
                        if (!response.ok) {
                            throw new Error('Помилка при відправці даних')
                        }

                        mainBlock.innerHTML = ``
                        targets()
                        return response.text()
                    })
                    .then(data => {
                        console.log(data)
                    })
                    .catch(error => {
                        console.error('Помилка:', error)
                    })
            })

            document.getElementById('competition-btn').addEventListener('click', () => {
                const competitionBtn = document.getElementById('competition-btn')
                fetch('/api/config/change-target-competition', {
                    method: 'GET'
                }).then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }
                    return response.json();
                })
                    .then(data => {
                        console.log(data)

                        if(data)
                            competitionBtn.innerHTML = `
                                <svg width="297" height="50" viewBox="0 0 297 50" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <rect width="297" height="50" rx="6" fill="url(#paint0_linear_18_1153)"/>
                                    <path d="M63.2891 32L58.2891 25.3239H56.4993V32H54.7379V17.4545H56.4993V23.7614H57.2663L63.0902 17.4545H65.1925L59.3118 23.8466L65.3913 32H63.2891ZM79.2567 24.7273C79.2567 26.2614 78.9798 27.5871 78.4258 28.7045C77.8718 29.822 77.1119 30.6837 76.146 31.2898C75.18 31.8958 74.0768 32.1989 72.8363 32.1989C71.5958 32.1989 70.4925 31.8958 69.5266 31.2898C68.5607 30.6837 67.8008 29.822 67.2468 28.7045C66.6928 27.5871 66.4158 26.2614 66.4158 24.7273C66.4158 23.1932 66.6928 21.8674 67.2468 20.75C67.8008 19.6326 68.5607 18.7708 69.5266 18.1648C70.4925 17.5587 71.5958 17.2557 72.8363 17.2557C74.0768 17.2557 75.18 17.5587 76.146 18.1648C77.1119 18.7708 77.8718 19.6326 78.4258 20.75C78.9798 21.8674 79.2567 23.1932 79.2567 24.7273ZM77.5522 24.7273C77.5522 23.4678 77.3415 22.4048 76.9201 21.5384C76.5034 20.6719 75.9376 20.0161 75.2227 19.571C74.5124 19.1259 73.717 18.9034 72.8363 18.9034C71.9556 18.9034 71.1578 19.1259 70.4428 19.571C69.7326 20.0161 69.1668 20.6719 68.7454 21.5384C68.3287 22.4048 68.1204 23.4678 68.1204 24.7273C68.1204 25.9867 68.3287 27.0497 68.7454 27.9162C69.1668 28.7827 69.7326 29.4384 70.4428 29.8835C71.1578 30.3286 71.9556 30.5511 72.8363 30.5511C73.717 30.5511 74.5124 30.3286 75.2227 29.8835C75.9376 29.4384 76.5034 28.7827 76.9201 27.9162C77.3415 27.0497 77.5522 25.9867 77.5522 24.7273ZM82.2184 32V17.4545H83.9798V23.9318H91.7354V17.4545H93.4968V32H91.7354V25.4943H83.9798V32H82.2184ZM105.574 32L100.574 25.3239H98.7844V32H97.0231V17.4545H98.7844V23.7614H99.5515L105.375 17.4545H107.478L101.597 23.8466L107.676 32H105.574ZM110.72 32.1989V30.5511H111.629C111.979 30.5511 112.273 30.4825 112.51 30.3452C112.751 30.2079 112.95 30.0303 113.106 29.8125C113.267 29.5947 113.4 29.3674 113.504 29.1307L113.731 28.5909L108.703 17.4545H110.606L114.612 26.4886L118.277 17.4545H120.152L115.123 29.642C114.924 30.0966 114.697 30.518 114.441 30.9062C114.19 31.2945 113.847 31.607 113.412 31.8438C112.981 32.0805 112.396 32.1989 111.657 32.1989H110.72ZM122.433 32V17.4545H127.348C128.489 17.4545 129.422 17.6605 130.146 18.0724C130.875 18.4796 131.415 19.0312 131.766 19.7273C132.116 20.4233 132.291 21.1998 132.291 22.0568C132.291 22.9138 132.116 23.6927 131.766 24.3935C131.42 25.0942 130.885 25.6529 130.161 26.0696C129.436 26.4815 128.508 26.6875 127.376 26.6875H123.854V25.125H127.32C128.101 25.125 128.728 24.9901 129.202 24.7202C129.675 24.4503 130.018 24.0857 130.232 23.6264C130.449 23.1624 130.558 22.6392 130.558 22.0568C130.558 21.4744 130.449 20.9536 130.232 20.4943C130.018 20.035 129.673 19.6752 129.195 19.4148C128.716 19.1496 128.082 19.017 127.291 19.017H124.195V32H122.433ZM135.896 32.1989V30.5511H136.805C137.155 30.5511 137.449 30.4825 137.685 30.3452C137.927 30.2079 138.126 30.0303 138.282 29.8125C138.443 29.5947 138.576 29.3674 138.68 29.1307L138.907 28.5909L133.879 17.4545H135.782L139.788 26.4886L143.452 17.4545H145.327L140.299 29.642C140.1 30.0966 139.873 30.518 139.617 30.9062C139.366 31.2945 139.023 31.607 138.587 31.8438C138.156 32.0805 137.572 32.1989 136.833 32.1989H135.896ZM147.609 32V17.4545H152.694C153.708 17.4545 154.543 17.6297 155.201 17.9801C155.859 18.3258 156.35 18.7921 156.672 19.3793C156.993 19.9616 157.154 20.608 157.154 21.3182C157.154 21.9432 157.043 22.4593 156.821 22.8665C156.603 23.2737 156.314 23.5956 155.954 23.8324C155.599 24.0691 155.213 24.2443 154.797 24.358V24.5C155.242 24.5284 155.689 24.6847 156.139 24.9688C156.589 25.2528 156.965 25.66 157.268 26.1903C157.571 26.7206 157.723 27.3693 157.723 28.1364C157.723 28.8655 157.557 29.5213 157.225 30.1037C156.894 30.6861 156.371 31.1477 155.656 31.4886C154.941 31.8295 154.011 32 152.865 32H147.609ZM149.37 30.4375H152.865C154.015 30.4375 154.832 30.215 155.315 29.7699C155.803 29.3201 156.047 28.7756 156.047 28.1364C156.047 27.6439 155.921 27.1894 155.67 26.7727C155.419 26.3513 155.062 26.0152 154.598 25.7642C154.134 25.5085 153.584 25.3807 152.95 25.3807H149.37V30.4375ZM149.37 23.8466H152.637C153.168 23.8466 153.646 23.7424 154.072 23.5341C154.503 23.3258 154.844 23.0322 155.095 22.6534C155.35 22.2746 155.478 21.8295 155.478 21.3182C155.478 20.679 155.256 20.1368 154.811 19.6918C154.366 19.242 153.66 19.017 152.694 19.017H149.37V23.8466ZM161.213 32H159.367L164.708 17.4545H166.526L171.867 32H170.02L165.674 19.7557H165.56L161.213 32ZM161.895 26.3182H169.338V27.8807H161.895V26.3182ZM171.638 19.017V17.4545H182.547V19.017H177.973V32H176.212V19.017H171.638ZM185.265 17.4545H186.998V28.9034H187.14L195.095 17.4545H196.799V32H195.038V20.5795H194.896L186.97 32H185.265V17.4545Z" fill="white"/>
                                    <path d="M38.75 23.375L29.375 14L20 23.375" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                                    <path d="M38.75 36.5L29.375 27.125L20 36.5" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                                    <rect x="210" y="6" width="77" height="38.8889" rx="19.4444" fill="#1D1F20"/>
                                    <rect x="249" y="9" width="32.6667" height="32.906" rx="16.3333" fill="url(#paint1_linear_18_1153)"/>
                                    <defs>
                                    <linearGradient id="paint0_linear_18_1153" x1="71.3382" y1="28.3133" x2="285.132" y2="21.2835" gradientUnits="userSpaceOnUse">
                                    <stop stop-color="#74CBBA"/>
                                    <stop offset="1" stop-color="#8DD294"/>
                                    </linearGradient>
                                    <linearGradient id="paint1_linear_18_1153" x1="256.846" y1="27.6335" x2="280.386" y2="27.5042" gradientUnits="userSpaceOnUse">
                                    <stop stop-color="#74CBBA"/>
                                    <stop offset="1" stop-color="#8DD294"/>
                                    </linearGradient>
                                    </defs>
                                </svg>
                            `
                        else
                            competitionBtn.innerHTML = `
                                <svg width="297" height="50" viewBox="0 0 297 50" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <rect width="297" height="50" rx="6" fill="#1D1F20"/>
                                    <path d="M63.2891 32L58.2891 25.3239H56.4993V32H54.7379V17.4545H56.4993V23.7614H57.2663L63.0902 17.4545H65.1925L59.3118 23.8466L65.3913 32H63.2891ZM79.2567 24.7273C79.2567 26.2614 78.9798 27.5871 78.4258 28.7045C77.8718 29.822 77.1119 30.6837 76.146 31.2898C75.18 31.8958 74.0768 32.1989 72.8363 32.1989C71.5958 32.1989 70.4925 31.8958 69.5266 31.2898C68.5607 30.6837 67.8008 29.822 67.2468 28.7045C66.6928 27.5871 66.4158 26.2614 66.4158 24.7273C66.4158 23.1932 66.6928 21.8674 67.2468 20.75C67.8008 19.6326 68.5607 18.7708 69.5266 18.1648C70.4925 17.5587 71.5958 17.2557 72.8363 17.2557C74.0768 17.2557 75.18 17.5587 76.146 18.1648C77.1119 18.7708 77.8718 19.6326 78.4258 20.75C78.9798 21.8674 79.2567 23.1932 79.2567 24.7273ZM77.5522 24.7273C77.5522 23.4678 77.3415 22.4048 76.9201 21.5384C76.5034 20.6719 75.9376 20.0161 75.2227 19.571C74.5124 19.1259 73.717 18.9034 72.8363 18.9034C71.9556 18.9034 71.1578 19.1259 70.4428 19.571C69.7326 20.0161 69.1668 20.6719 68.7454 21.5384C68.3287 22.4048 68.1204 23.4678 68.1204 24.7273C68.1204 25.9867 68.3287 27.0497 68.7454 27.9162C69.1668 28.7827 69.7326 29.4384 70.4428 29.8835C71.1578 30.3286 71.9556 30.5511 72.8363 30.5511C73.717 30.5511 74.5124 30.3286 75.2227 29.8835C75.9376 29.4384 76.5034 28.7827 76.9201 27.9162C77.3415 27.0497 77.5522 25.9867 77.5522 24.7273ZM82.2184 32V17.4545H83.9798V23.9318H91.7354V17.4545H93.4968V32H91.7354V25.4943H83.9798V32H82.2184ZM105.574 32L100.574 25.3239H98.7844V32H97.0231V17.4545H98.7844V23.7614H99.5515L105.375 17.4545H107.478L101.597 23.8466L107.676 32H105.574ZM110.72 32.1989V30.5511H111.629C111.979 30.5511 112.273 30.4825 112.51 30.3452C112.751 30.2079 112.95 30.0303 113.106 29.8125C113.267 29.5947 113.4 29.3674 113.504 29.1307L113.731 28.5909L108.703 17.4545H110.606L114.612 26.4886L118.277 17.4545H120.152L115.123 29.642C114.924 30.0966 114.697 30.518 114.441 30.9062C114.19 31.2945 113.847 31.607 113.412 31.8438C112.981 32.0805 112.396 32.1989 111.657 32.1989H110.72ZM122.433 32V17.4545H127.348C128.489 17.4545 129.422 17.6605 130.146 18.0724C130.875 18.4796 131.415 19.0312 131.766 19.7273C132.116 20.4233 132.291 21.1998 132.291 22.0568C132.291 22.9138 132.116 23.6927 131.766 24.3935C131.42 25.0942 130.885 25.6529 130.161 26.0696C129.436 26.4815 128.508 26.6875 127.376 26.6875H123.854V25.125H127.32C128.101 25.125 128.728 24.9901 129.202 24.7202C129.675 24.4503 130.018 24.0857 130.232 23.6264C130.449 23.1624 130.558 22.6392 130.558 22.0568C130.558 21.4744 130.449 20.9536 130.232 20.4943C130.018 20.035 129.673 19.6752 129.195 19.4148C128.716 19.1496 128.082 19.017 127.291 19.017H124.195V32H122.433ZM135.896 32.1989V30.5511H136.805C137.155 30.5511 137.449 30.4825 137.685 30.3452C137.927 30.2079 138.126 30.0303 138.282 29.8125C138.443 29.5947 138.576 29.3674 138.68 29.1307L138.907 28.5909L133.879 17.4545H135.782L139.788 26.4886L143.452 17.4545H145.327L140.299 29.642C140.1 30.0966 139.873 30.518 139.617 30.9062C139.366 31.2945 139.023 31.607 138.587 31.8438C138.156 32.0805 137.572 32.1989 136.833 32.1989H135.896ZM147.609 32V17.4545H152.694C153.708 17.4545 154.543 17.6297 155.201 17.9801C155.859 18.3258 156.35 18.7921 156.672 19.3793C156.993 19.9616 157.154 20.608 157.154 21.3182C157.154 21.9432 157.043 22.4593 156.821 22.8665C156.603 23.2737 156.314 23.5956 155.954 23.8324C155.599 24.0691 155.213 24.2443 154.797 24.358V24.5C155.242 24.5284 155.689 24.6847 156.139 24.9688C156.589 25.2528 156.965 25.66 157.268 26.1903C157.571 26.7206 157.723 27.3693 157.723 28.1364C157.723 28.8655 157.557 29.5213 157.225 30.1037C156.894 30.6861 156.371 31.1477 155.656 31.4886C154.941 31.8295 154.011 32 152.865 32H147.609ZM149.37 30.4375H152.865C154.015 30.4375 154.832 30.215 155.315 29.7699C155.803 29.3201 156.047 28.7756 156.047 28.1364C156.047 27.6439 155.921 27.1894 155.67 26.7727C155.419 26.3513 155.062 26.0152 154.598 25.7642C154.134 25.5085 153.584 25.3807 152.95 25.3807H149.37V30.4375ZM149.37 23.8466H152.637C153.168 23.8466 153.646 23.7424 154.072 23.5341C154.503 23.3258 154.844 23.0322 155.095 22.6534C155.35 22.2746 155.478 21.8295 155.478 21.3182C155.478 20.679 155.256 20.1368 154.811 19.6918C154.366 19.242 153.66 19.017 152.694 19.017H149.37V23.8466ZM161.213 32H159.367L164.708 17.4545H166.526L171.867 32H170.02L165.674 19.7557H165.56L161.213 32ZM161.895 26.3182H169.338V27.8807H161.895V26.3182ZM171.638 19.017V17.4545H182.547V19.017H177.973V32H176.212V19.017H171.638ZM185.265 17.4545H186.998V28.9034H187.14L195.095 17.4545H196.799V32H195.038V20.5795H194.896L186.97 32H185.265V17.4545Z" fill="white"/>
                                    <path d="M38.75 23.375L29.375 14L20 23.375" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                                    <path d="M38.75 36.5L29.375 27.125L20 36.5" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                                    <rect x="210" y="6" width="77" height="38.8889" rx="19.4444" fill="#232627"/>
                                    <rect x="214.667" y="8.99146" width="32.6667" height="32.906" rx="16.3333" fill="url(#paint0_linear_18_1144)"/>
                                    <defs>
                                    <linearGradient id="paint0_linear_18_1144" x1="222.513" y1="27.625" x2="246.053" y2="27.4956" gradientUnits="userSpaceOnUse">
                                    <stop stop-color="#74CBBA"/>
                                    <stop offset="1" stop-color="#8DD294"/>
                                    </linearGradient>
                                    </defs>
                                </svg>
                            `
                    })

            })
        })
}
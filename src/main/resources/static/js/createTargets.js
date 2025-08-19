const mainBlock = document.getElementById('main-block')

export function createTargets() {
    const signal = controllerCreateTargets.signal

    const table = document.createElement('div')
    table.className = 'table-offers'
    table.innerHTML = `
        <div></div>
        <div class="back-header"><div class="header">НАЗВА</div></div>
        <div class="back-header"><div class="header">МІН.ПОРІГ</div></div>
        <div class="back-header"><div class="header">МАКС.ПОРІГ</div></div>
        <div class="back-header"><div class="header">МАКС. ТАРГЕТ</div></div>
        <div class="back-header"><div class="header">DM MIN <img src="/img/lock.png" alt="lock" class="lock-icon"></div></div>
        <div class="back-header"><div class="header">DM MIN <img src="/img/green lock.png" alt="lock" class="lock-icon"></div></div>`

    mainBlock.appendChild(table)

    document.getElementById('plus-line').addEventListener('click', () => {
        table.insertAdjacentHTML('beforeend', `
            <div></div> 
            <div class="cell"><input type="text" value=""></div>
            <div class="cell"><input type="number" value="" readonly></div>
            <div class="cell"><input type="number" value="" readonly></div>
            <div class="cell"><input type="text" value="" readonly></div>
            <div class="cell"><input type="text" value="" readonly></div>
            <div class="cell"><input type="text" value="" readonly></div>
        `);
    })

    document.getElementById('update-info').addEventListener('click', () => {
        const resultArray = collectTargets()
        console.log(resultArray)
        fetch('/api/targets/update-info', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(resultArray),
            signal: signal
        })
            .then(response => response.json())
            .then(data => {
                console.log(data)

                mainBlock.innerHTML = ``

                table.className = 'table-offers'
                table.innerHTML = `
                    <div></div>
                    <div class="header">НАЗВА ПРЕДМЕТА</div>
                    <div class="header">МІНІМАЛЬНИЙ ПОРІГ</div>
                    <div class="header">МАКСИМАЛЬНИЙ ПОРІГ</div>
                    <div class="header">МАКС. ТАРГЕТ</div>
                    <div class="header">DM МІН. ЛОК</div>
                    <div class="header">DM МІН. РОЗЛОК</div>`

                mainBlock.appendChild(table)

                // data.forEach(item => {
                //     table.insertAdjacentHTML('beforeend', `
                //         <div class="cell"><img src="${item.imageLink}" alt="lock" class="skin-img" data-asset="${item.id}" data-type="image"></div>
                //         <div class="cell"><input type="text" value="${item.name}" readonly data-asset="${item.id}" data-type="name"></div>
                //         <div class="cell"><input type="number" value="" data-asset="${item.id}" data-type="minPrice"></div>
                //         <div class="cell"><input type="number" value="" data-asset="${item.id}" data-type="maxPrice"></div>
                //         <div class="cell" id="target-${item.id}"><input type="text" value="${item.maxTarget}" readonly data-asset="${item.id}" data-type="maxTarget"></div>
                //         <div class="cell"><input type="text" value="${item.minWithLock}" readonly data-asset="${item.id}" data-type="locked"></div>
                //         <div class="cell"><input type="text" value="${item.minWithoutLock}" readonly data-asset="${item.id}" data-type="unlocked"></div>
                //     `)
                // })

                data.forEach(item => {
                    table.insertAdjacentHTML('beforeend', `
                        <div class="cell"><img src="${item.imageLink}" alt="lock" class="skin-img" data-asset="${item.id}" data-type="image"></div>
                        <div class="cell"><input type="text" value="${item.name}" readonly data-asset="${item.id}" data-type="name"></div>
                        <div class="cell"><input type="number" value="" data-asset="${item.id}" data-type="minPrice"></div>
                        <div class="cell"><input type="number" value="" data-asset="${item.id}" data-type="maxPrice"></div>
                        <div class="cell tooltip" id="target-${item.id}">
                            <input type="text" value="${item.maxTarget}" readonly data-asset="${item.id}" data-type="maxTarget">
                            <div class="popup-table">
                                <table>
                                    <tr><th>Ціна</th><th>Кількість</th></tr>
                                    ${(item.targets || []).map(t => `
                                        <tr>
                                            <td>${t.price}</td>
                                            <td>${t.quantity}</td>
                                        </tr>
                                    `).join('')}
                                </table>
                            </div>
                        </div>                        
                        <div class="cell"><input type="text" value="${item.minWithLock}" readonly data-asset="${item.id}" data-type="locked"></div>
                        <div class="cell"><input type="text" value="${item.minWithoutLock}" readonly data-asset="${item.id}" data-type="unlocked"></div>
                    `)
                })

                document.getElementById('create-targets').addEventListener('click', () => {
                    const inputs = document.querySelectorAll('input[data-asset]')
                    const results = {}

                    inputs.forEach(input => {
                        const assetId = input.dataset.asset
                        const type = input.dataset.type
                        const value = input.value

                        if (!results[assetId]) {
                            results[assetId] = { assetId }
                        }

                        switch (type) {
                            case "name":
                                results[assetId].name = value
                                break;
                            case "maxPrice":
                                results[assetId].maxPrice = value
                                break;
                            case "minPrice":
                                results[assetId].minPrice = value
                                results[assetId].price = value
                                break;
                            case "maxTarget":
                                results[assetId].maxTarget = value
                                break;
                            case "locked":
                                results[assetId].minWithLock = value
                                break;
                            case "unlocked":
                                results[assetId].minWithoutLock = value
                                break;
                        }
                    })

                    const images = document.querySelectorAll('img[data-asset]')
                    images.forEach(img => {
                        const assetId = img.dataset.asset
                        if (results[assetId]) {
                            results[assetId].imageLink = img.src
                        }
                    })

                    const resultArray = Object.values(results)

                    console.log(resultArray)

                    alert("Targets will be created...")

                    fetch('/api/targets/create', {
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
                            await sleep(1000);
                            createTargets()
                            return response.text()
                        })
                        .then(data => {
                            console.log(data)
                        })
                        .catch(error => {
                            console.error('Помилка:', error)
                        })
                })


            })
            .catch(error => {
                console.error('Помилка:', error)
            })
    })


    const fileInput = document.getElementById('fileInput')
    const fileLabel = document.querySelector('label.file-upload')

    function ensureStatusNode() {
        let node = fileLabel.nextElementSibling
        if (!node || !node.classList.contains('upload-status')) {
            node = document.createElement('div')
            node.className = 'upload-status'
            node.innerHTML = `
      <span class="status-icon" aria-hidden="true">⏳</span>
      <div class="progress" aria-label="Прогрес завантаження">
        <div class="progress__bar"></div>
      </div>
      <div class="progress__percent" aria-live="polite">0%</div>
    `
            fileLabel.after(node)
        }
        return node
    }

    function setStatusProgress(node, percent) {
        const bar = node.querySelector('.progress__bar')
        const pct = node.querySelector('.progress__percent')
        const icon = node.querySelector('.status-icon')
        icon.textContent = '⏳'
        icon.className = 'status-icon'
        bar.style.width = `${Math.max(0, Math.min(100, percent))}%`
        pct.textContent = `${Math.max(0, Math.min(100, percent))}%`
    }

    function setStatusDone(node, msg = "Успішно оброблено"){
        const bar = node.querySelector('.progress__bar')
        const pct = node.querySelector('.progress__percent')
        const icon = node.querySelector('.status-icon')
        icon.textContent = '✅'
        icon.className = 'status-icon status-icon--ok'
        bar.style.width = '100%'
        pct.textContent = msg
        pct.style.color = '#8DD294'
    }

    function setStatusError(node, msg = "Помилка обробки") {
        const icon = node.querySelector('.status-icon')
        const pct = node.querySelector('.progress__percent')
        icon.textContent = '❌'
        icon.className = 'status-icon status-icon--err'
        pct.textContent = msg
        pct.style.color = '#ff6b6b'
    }

    function uploadFiles(files) {
        const fd = new FormData()
        for (const f of files) fd.append('file', f)

        const node = ensureStatusNode()
        setStatusProgress(node, 0)

        const xhr = new XMLHttpRequest()
        xhr.open('POST', '/api/targets/upload')

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100)
                setStatusProgress(node, percent)
            }
        }

        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                setStatusDone(node)
            } else {
                setStatusError(node)
            }
        }

        xhr.onerror = () => setStatusError(node, "Помилка мережі")

        xhr.send(fd)
    }

    fileInput.addEventListener('change', () => {
        if (fileInput.files?.length) uploadFiles(Array.from(fileInput.files))
    });

    ['dragenter','dragover'].forEach(evt =>
        fileLabel.addEventListener(evt, e => {
            e.preventDefault(); e.stopPropagation()
            fileLabel.style.filter = 'brightness(0.95)'
        })
    );
    ['dragleave','drop'].forEach(evt =>
        fileLabel.addEventListener(evt, e => {
            e.preventDefault(); e.stopPropagation()
            fileLabel.style.filter = ''
        })
    );
    fileLabel.addEventListener('drop', e => {
        const dt = e.dataTransfer
        if (dt?.files?.length) uploadFiles(Array.from(dt.files))
    });
}

function collectTargets() {
    const table = document.querySelector('.table-offers');
    const inputs = table.querySelectorAll('input');

    const rows = [];
    for (let i = 0; i < inputs.length; i += 6) {
        const name = inputs[i].value.trim();

        if (name === '') continue;

        rows.push(name);
    }

    return rows;
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
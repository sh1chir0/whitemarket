const mainBlock = document.getElementById('main-block')

// guards проти дабл-кліків / дубль-листенерів
let isUpdateInfoRunning = false
let isCreateTargetsRunning = false
let isUploadRunning = false

export function createTargets() {
    const signal = controllerCreateTargets.signal

    // Перерендер блоку (щоб не накопичувались таблиці)
    mainBlock.innerHTML = ''

    const table = document.createElement('div')
    table.className = 'table-offers'
    table.innerHTML = `
    <div></div>
    <div class="back-header"><div class="header">НАЗВА ПРЕДМЕТА</div></div>
    <div class="back-header"><div class="header">МІНІМАЛЬНИЙ ПОРІГ</div></div>
    <div class="back-header"><div class="header">МАКСИМАЛЬНИЙ ПОРІГ</div></div>
    <div class="back-header"><div class="header">МАКС. ТАРГЕТ WM</div></div>
    <div class="back-header"><div class="header">МІН. WM</div></div>
  `
    mainBlock.appendChild(table)

    const plusBtn = document.getElementById('plus-line')
    if (plusBtn) {
        plusBtn.onclick = () => {
            table.insertAdjacentHTML('beforeend', `
        <div></div> 
        <div class="cell"><input type="text" value=""></div>
        <div class="cell"><input type="number" value="" readonly></div>
        <div class="cell"><input type="number" value="" readonly></div>
        <div class="cell"><input type="text" value="" readonly></div>
        <div class="cell"><input type="text" value="" readonly></div>
      `)
        }
    }

    // ====== UPDATE INFO (guard + disable) ======
    const updateBtn = document.getElementById('update-info')
    if (updateBtn) {
        updateBtn.onclick = async () => {
            if (isUpdateInfoRunning) return
            isUpdateInfoRunning = true
            updateBtn.disabled = true

            try {
                const resultArray = collectTargets()
                console.log(resultArray)

                const resp = await fetch('/api/targets/update-info', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(resultArray),
                    signal: signal
                })

                const data = await resp.json()
                console.log(data)

                // перерендер таблиці з даними
                mainBlock.innerHTML = ''

                table.className = 'table-offers'
                table.innerHTML = `
          <div></div>
          <div class="header">НАЗВА ПРЕДМЕТА</div>
          <div class="header">МІНІМАЛЬНИЙ ПОРІГ</div>
          <div class="header">МАКСИМАЛЬНИЙ ПОРІГ</div>
          <div class="header">МАКС. ТАРГЕТ WM</div>
          <div class="header">МІН. WM</div>
        `
                mainBlock.appendChild(table)

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
<!--            <div class="cell"><input type="text" value="${item.maxTarget}" readonly data-asset="${item.id}" data-type="maxTarget"></div>-->
            <div class="cell"><input type="text" value="${item.minWM}" readonly data-asset="${item.id}" data-type="minWM"></div>
          `)
                })

                bindCreateTargetsButton(signal)

            } catch (error) {
                console.error('Помилка:', error)
            } finally {
                isUpdateInfoRunning = false
                updateBtn.disabled = false
            }
        }
    }

    // ====== CREATE TARGETS (підв'язуємо одразу теж, якщо кнопка доступна) ======
    bindCreateTargetsButton(signal)

    // ====== UPLOAD (щоб не дублювалося — тільки onchange/ondrop/on... ) ======
    bindUpload()
}

function bindCreateTargetsButton(signal) {
    const btn = document.getElementById('create-targets')
    if (!btn) return

    btn.onclick = async () => {
        if (isCreateTargetsRunning) return
        isCreateTargetsRunning = true
        btn.disabled = true

        try {
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
                        break
                    case "maxPrice":
                        results[assetId].maxPrice = value
                        break
                    case "minPrice":
                        results[assetId].minPrice = value
                        results[assetId].price = value
                        break
                    case "maxTarget":
                        results[assetId].maxTarget = value
                        break
                    case "minWM":
                        results[assetId].minWM = value
                        break
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

            const response = await fetch('/api/targets/create', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(resultArray),
                signal: signal
            })

            if (!response.ok) {
                throw new Error('Помилка при відправці даних')
            }

            mainBlock.innerHTML = ``
            await sleep(1000)
            createTargets()

            const data = await response.text()
            console.log(data)

        } catch (error) {
            console.error('Помилка:', error)
        } finally {
            isCreateTargetsRunning = false
            btn.disabled = false
        }
    }
}

function bindUpload() {
    const fileInput = document.getElementById('fileInput')
    const fileLabel = document.querySelector('label.file-upload')
    if (!fileInput || !fileLabel) return

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

    function setStatusDone(node, msg = "Успішно оброблено") {
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
        if (isUploadRunning) return
        isUploadRunning = true

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
            isUploadRunning = false
        }

        xhr.onerror = () => {
            setStatusError(node, "Помилка мережі")
            isUploadRunning = false
        }

        xhr.send(fd)
    }

    // onchange перезаписує попередній handler
    fileInput.onchange = () => {
        if (fileInput.files?.length) uploadFiles(Array.from(fileInput.files))
    }

    // drag&drop (перезаписуємо on* щоб не множились)
    fileLabel.ondragenter = (e) => {
        e.preventDefault(); e.stopPropagation()
        fileLabel.style.filter = 'brightness(0.95)'
    }
    fileLabel.ondragover = (e) => {
        e.preventDefault(); e.stopPropagation()
        fileLabel.style.filter = 'brightness(0.95)'
    }
    fileLabel.ondragleave = (e) => {
        e.preventDefault(); e.stopPropagation()
        fileLabel.style.filter = ''
    }
    fileLabel.ondrop = (e) => {
        e.preventDefault(); e.stopPropagation()
        fileLabel.style.filter = ''
        const dt = e.dataTransfer
        if (dt?.files?.length) uploadFiles(Array.from(dt.files))
    }
}

function collectTargets() {
    const table = document.querySelector('.table-offers')
    if (!table) return []

    const inputs = table.querySelectorAll('input')
    const rows = []

    for (let i = 0; i < inputs.length; i += 5) {
        const name = inputs[i]?.value?.trim?.() ?? ''
        if (name === '') continue
        rows.push(name)
    }

    return rows
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}


// const mainBlock = document.getElementById('main-block')
//
// export function createTargets() {
//     const signal = controllerCreateTargets.signal
//
//     const table = document.createElement('div')
//     table.className = 'table-offers'
//     table.innerHTML = `
//         <div></div>
//                     <div class="header">НАЗВА ПРЕДМЕТА</div>
//                     <div class="header">МІНІМАЛЬНИЙ ПОРІГ</div>
//                     <div class="header">МАКСИМАЛЬНИЙ ПОРІГ</div>
//                     <div class="header">МАКС. ТАРГЕТ WM</div>
//                     <div class="header">МІН. WM</div>`
//
//     mainBlock.appendChild(table)
//
//     document.getElementById('plus-line').addEventListener('click', () => {
//         table.insertAdjacentHTML('beforeend', `
//             <div></div>
//             <div class="cell"><input type="text" value=""></div>
//             <div class="cell"><input type="number" value="" readonly></div>
//             <div class="cell"><input type="number" value="" readonly></div>
//             <div class="cell"><input type="text" value="" readonly></div>
//             <div class="cell"><input type="text" value="" readonly></div>
//         `);
//     })
//
//     document.getElementById('update-info').addEventListener('click', () => {
//         const resultArray = collectTargets()
//         console.log(resultArray)
//         fetch('/api/targets/update-info', {
//             method: 'POST',
//             headers: {
//                 'Content-Type': 'application/json'
//             },
//             body: JSON.stringify(resultArray),
//             signal: signal
//         })
//             .then(response => response.json())
//             .then(data => {
//                 console.log(data)
//
//                 mainBlock.innerHTML = ``
//
//                 table.className = 'table-offers'
//                 table.innerHTML = `
//                     <div></div>
//                     <div class="header">НАЗВА ПРЕДМЕТА</div>
//                     <div class="header">МІНІМАЛЬНИЙ ПОРІГ</div>
//                     <div class="header">МАКСИМАЛЬНИЙ ПОРІГ</div>
//                     <div class="header">МАКС. ТАРГЕТ WM</div>
//                     <div class="header">МІН. WM</div>`
//
//                 mainBlock.appendChild(table)
//
//                 // <div class="cell tooltip" id="target-${item.id}">
//                 //     <input type="text" value="${item.maxTarget}" readonly data-asset="${item.id}" data-type="maxTarget">
//                 //     <div class="popup-table">
//                 //         <table>
//                 //             <tr><th>Ціна</th><th>Кількість</th></tr>
//                 //             ${(item.targets || []).map(t => `
//                 //                 <tr>
//                 //                     <td>${t.price}</td>
//                 //                     <td>${t.quantity}</td>
//                 //                 </tr>
//                 //             `).join('')}
//                 //         </table>
//                 //     </div>
//                 // </div>
//
//                 data.forEach(item => {
//                     table.insertAdjacentHTML('beforeend', `
//                         <div class="cell"><img src="${item.imageLink}" alt="lock" class="skin-img" data-asset="${item.id}" data-type="image"></div>
//                         <div class="cell"><input type="text" value="${item.name}" readonly data-asset="${item.id}" data-type="name"></div>
//                         <div class="cell"><input type="number" value="" data-asset="${item.id}" data-type="minPrice"></div>
//                         <div class="cell"><input type="number" value="" data-asset="${item.id}" data-type="maxPrice"></div>
//
//                         <div class="cell"><input type="text" value="${item.maxTarget}" readonly data-asset="${item.id}" data-type="maxTarget"></div>
//                         <div class="cell"><input type="text" value="${item.minWM}" readonly data-asset="${item.id}" data-type="minWM"></div>
//                     `)
//                 })
//
//                 document.getElementById('create-targets').addEventListener('click', () => {
//                     const inputs = document.querySelectorAll('input[data-asset]')
//                     const results = {}
//
//                     inputs.forEach(input => {
//                         const assetId = input.dataset.asset
//                         const type = input.dataset.type
//                         const value = input.value
//
//                         if (!results[assetId]) {
//                             results[assetId] = { assetId }
//                         }
//
//                         switch (type) {
//                             case "name":
//                                 results[assetId].name = value
//                                 break;
//                             case "maxPrice":
//                                 results[assetId].maxPrice = value
//                                 break;
//                             case "minPrice":
//                                 results[assetId].minPrice = value
//                                 results[assetId].price = value
//                                 break;
//                             case "maxTarget":
//                                 results[assetId].maxTarget = value
//                                 break;
//                             case "minWM":
//                                 results[assetId].minWM = value
//                                 break;
//                         }
//                     })
//
//                     const images = document.querySelectorAll('img[data-asset]')
//                     images.forEach(img => {
//                         const assetId = img.dataset.asset
//                         if (results[assetId]) {
//                             results[assetId].imageLink = img.src
//                         }
//                     })
//
//                     const resultArray = Object.values(results)
//
//                     console.log(resultArray)
//
//                     alert("Targets will be created...")
//
//                     fetch('/api/targets/create', {
//                         method: 'POST',
//                         headers: {
//                             'Content-Type': 'application/json'
//                         },
//                         body: JSON.stringify(resultArray)
//                     })
//                         .then(async response => {
//                             if (!response.ok) {
//                                 throw new Error('Помилка при відправці даних')
//                             }
//
//                             mainBlock.innerHTML = ``
//                             await sleep(1000);
//                             createTargets()
//                             return response.text()
//                         })
//                         .then(data => {
//                             console.log(data)
//                         })
//                         .catch(error => {
//                             console.error('Помилка:', error)
//                         })
//                 })
//
//
//             })
//             .catch(error => {
//                 console.error('Помилка:', error)
//             })
//     })
//
//
//     const fileInput = document.getElementById('fileInput')
//     const fileLabel = document.querySelector('label.file-upload')
//
//     function ensureStatusNode() {
//         let node = fileLabel.nextElementSibling
//         if (!node || !node.classList.contains('upload-status')) {
//             node = document.createElement('div')
//             node.className = 'upload-status'
//             node.innerHTML = `
//       <span class="status-icon" aria-hidden="true">⏳</span>
//       <div class="progress" aria-label="Прогрес завантаження">
//         <div class="progress__bar"></div>
//       </div>
//       <div class="progress__percent" aria-live="polite">0%</div>
//     `
//             fileLabel.after(node)
//         }
//         return node
//     }
//
//     function setStatusProgress(node, percent) {
//         const bar = node.querySelector('.progress__bar')
//         const pct = node.querySelector('.progress__percent')
//         const icon = node.querySelector('.status-icon')
//         icon.textContent = '⏳'
//         icon.className = 'status-icon'
//         bar.style.width = `${Math.max(0, Math.min(100, percent))}%`
//         pct.textContent = `${Math.max(0, Math.min(100, percent))}%`
//     }
//
//     function setStatusDone(node, msg = "Успішно оброблено"){
//         const bar = node.querySelector('.progress__bar')
//         const pct = node.querySelector('.progress__percent')
//         const icon = node.querySelector('.status-icon')
//         icon.textContent = '✅'
//         icon.className = 'status-icon status-icon--ok'
//         bar.style.width = '100%'
//         pct.textContent = msg
//         pct.style.color = '#8DD294'
//     }
//
//     function setStatusError(node, msg = "Помилка обробки") {
//         const icon = node.querySelector('.status-icon')
//         const pct = node.querySelector('.progress__percent')
//         icon.textContent = '❌'
//         icon.className = 'status-icon status-icon--err'
//         pct.textContent = msg
//         pct.style.color = '#ff6b6b'
//     }
//
//     function uploadFiles(files) {
//         const fd = new FormData()
//         for (const f of files) fd.append('file', f)
//
//         const node = ensureStatusNode()
//         setStatusProgress(node, 0)
//
//         const xhr = new XMLHttpRequest()
//         xhr.open('POST', '/api/targets/upload')
//
//         xhr.upload.onprogress = (e) => {
//             if (e.lengthComputable) {
//                 const percent = Math.round((e.loaded / e.total) * 100)
//                 setStatusProgress(node, percent)
//             }
//         }
//
//         xhr.onload = () => {
//             if (xhr.status >= 200 && xhr.status < 300) {
//                 setStatusDone(node)
//             } else {
//                 setStatusError(node)
//             }
//         }
//
//         xhr.onerror = () => setStatusError(node, "Помилка мережі")
//
//         xhr.send(fd)
//     }
//
//     fileInput.addEventListener('change', () => {
//         if (fileInput.files?.length) uploadFiles(Array.from(fileInput.files))
//     });
//
//     ['dragenter','dragover'].forEach(evt =>
//         fileLabel.addEventListener(evt, e => {
//             e.preventDefault(); e.stopPropagation()
//             fileLabel.style.filter = 'brightness(0.95)'
//         })
//     );
//     ['dragleave','drop'].forEach(evt =>
//         fileLabel.addEventListener(evt, e => {
//             e.preventDefault(); e.stopPropagation()
//             fileLabel.style.filter = ''
//         })
//     );
//     fileLabel.addEventListener('drop', e => {
//         const dt = e.dataTransfer
//         if (dt?.files?.length) uploadFiles(Array.from(dt.files))
//     });
// }
//
// function collectTargets() {
//     const table = document.querySelector('.table-offers');
//     const inputs = table.querySelectorAll('input');
//
//     const rows = [];
//     for (let i = 0; i < inputs.length; i += 6) {
//         const name = inputs[i].value.trim();
//
//         if (name === '') continue;
//
//         rows.push(name);
//     }
//
//     return rows;
// }
//
// function sleep(ms) {
//     return new Promise(resolve => setTimeout(resolve, ms));
// }
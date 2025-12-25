const mainBlock = document.getElementById('main-block')

let soldHandlerAttached = false;

export function inventories() {
    const signal = controllerInventory.signal

    fetch('/api/inventories/get-inventories', { method: 'GET', signal })
        .then(r => r.json())
        .then(data => {

            mainBlock.innerHTML = '';               // <-- важливо, щоб не накопичувалось
            const table = document.createElement('div')
            table.className = 'table-inv'
            table.innerHTML = `
        <div></div>
        <div class="back-header"><div class="header">НАЗВА</div></div>
        <div class="back-header"><div class="header">МІН.ПОРІГ</div></div>
        <div class="back-header"><div class="header">МАКС.ПОРІГ</div></div>
        <div class="back-header"><div class="header">МІН. WM</div></div>
      `
            data.forEach(item => {
                table.innerHTML += `
          <div class="cell"><img src="${item.imageLink}" class="skin-img"
               data-asset="${item.assetId}" data-inventory="${item.inventoryId || ''}" data-type="image"></div>
          <div class="cell"><input type="text" style="text-align:left" value="${item.name}" readonly
               data-asset="${item.assetId}" data-inventory="${item.inventoryId || ''}" data-type="name"
               data-tradable="${item.tradable}"></div>
          <div class="cell"><input type="number" value=""
               data-asset="${item.assetId}" data-inventory="${item.inventoryId || ''}" data-type="minPrice"></div>
          <div class="cell"><input type="number" value=""
               data-asset="${item.assetId}" data-inventory="${item.inventoryId || ''}" data-type="maxPrice"></div>
          <div class="cell"><input type="text" value="${item.minWM}" readonly
               data-asset="${item.assetId}" data-inventory="${item.inventoryId || ''}" data-type="minWM"></div>
        `
            })
            mainBlock.appendChild(table)

            attachSoldHandler(signal)
        })
}

let isSubmitting = false;

function attachSoldHandler(signal) {
    const btn = document.getElementById('sold')
    if (!btn) return

    btn.removeEventListener('click', onSoldClick)
    btn.addEventListener('click', (e) => onSoldClick(e, signal))
}

async function onSoldClick(e, signal) {
    e.preventDefault()

    if (isSubmitting) return
    isSubmitting = true

    const btn = document.getElementById('sold')
    if (btn) btn.disabled = true

    try {
        const inputs = document.querySelectorAll('input[data-asset]')
        const results = {}

        inputs.forEach(input => {
            const assetId = input.dataset.asset
            const type = input.dataset.type
            const value = input.value
            const inventoryId = input.dataset.inventory

            if (!results[assetId]) results[assetId] = { assetId }

            if (inventoryId && (!results[assetId].inventoryId || results[assetId].inventoryId.trim() === '')) {
                results[assetId].inventoryId = inventoryId
            }

            if (type === 'name') {
                results[assetId].name = value
                results[assetId].tradable = input.dataset.tradable === 'true'
            } else {
                results[assetId][type] = value
                if (type === 'maxPrice') results[assetId].price = value
                if (type === 'minWM') results[assetId].minWM = value
            }
        })

        const images = document.querySelectorAll('img[data-asset]')
        images.forEach(img => {
            const assetId = img.dataset.asset
            if (results[assetId]) {
                results[assetId].imageLink = img.src
                const inv = img.dataset.inventory
                if (inv && (!results[assetId].inventoryId || results[assetId].inventoryId.trim() === '')) {
                    results[assetId].inventoryId = inv
                }
            }
        })

        const resultArray = Object.values(results).filter(item => item.maxPrice && item.maxPrice.trim() !== '')

        for (const item of resultArray) {
            const max = parseFloat(item.maxPrice)
            const minWM = parseFloat(item.minWM || 0)
            if (!isNaN(max) && max < minWM) {
                const proceed = confirm(`Для "${item.name || item.assetId}": Макс.поріг < мін.WM. Продовжити?`)
                if (!proceed) return
            }
        }

        const response = await fetch('/api/offers/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(resultArray),
            signal
        })

        if (!response.ok) throw new Error('Помилка при відправці даних')

        mainBlock.innerHTML = ''
        await sleep(2000)
        inventories()

        const txt = await response.text()
        console.log(txt)

    } catch (error) {
        console.error('Помилка:', error)
    } finally {
        isSubmitting = false
        if (btn) btn.disabled = false
    }
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}


// const mainBlock = document.getElementById('main-block')
//
// export function inventories() {
//     const signal = controllerInventory.signal
//     fetch('/api/inventories/get-inventories', {
//         method: 'GET',
//         signal: signal
//     }).then(response => response.json())
//         .then(data => {
//             const table = document.createElement('div')
//             table.className = 'table-inv'
//             table.innerHTML = `
//                 <div></div>
//                 <div class="back-header"><div class="header">НАЗВА</div></div>
//                 <div class="back-header"><div class="header">МІН.ПОРІГ</div></div>
//                 <div class="back-header"><div class="header">МАКС.ПОРІГ</div></div>
//                 <div class="back-header"><div class="header">МІН. WM</div></div>
//             `
//             data.forEach(item => {
//                 table.innerHTML += `
//             <div class="cell"><img src="${item.imageLink}" alt="lock" class="skin-img" data-asset="${item.assetId}" data-inventory="${item.inventoryId || ''}" data-type="image"></div>
//             <div class="cell"><input type="text" style="text-align: left;" value="${item.name}" readonly data-asset="${item.assetId}" data-inventory="${item.inventoryId || ''}" data-type="name" data-tradable="${item.tradable}"></div>
//             <div class="cell"><input type="number" value="" data-asset="${item.assetId}" data-inventory="${item.inventoryId || ''}" data-type="minPrice"></div>
//             <div class="cell"><input type="number" value="" data-asset="${item.assetId}" data-inventory="${item.inventoryId || ''}" data-type="maxPrice"></div>
//             <div class="cell"><input type="text" value="${item.minWM}" readonly data-asset="${item.assetId}" data-inventory="${item.inventoryId || ''}" data-type="minWM"></div>
//         `
//             })
//
//             mainBlock.appendChild(table)
//
//             document.getElementById('sold').addEventListener('click', async () => {
//                 const inputs = document.querySelectorAll('input[data-asset]')
//                 const results = {}
//
//                 inputs.forEach(input => {
//                     const assetId = input.dataset.asset
//                     const type = input.dataset.type
//                     const value = input.value
//                     const inventoryId = input.dataset.inventory;
//
//                     if (!results[assetId]) {
//                         results[assetId] = {assetId}
//                     }
//
//                     if (inventoryId && (!results[assetId].inventoryId || results[assetId].inventoryId.trim() === '')) {
//                         results[assetId].inventoryId = inventoryId;
//                     }
//
//                     if (type === 'name') {
//                         results[assetId].name = value
//                         results[assetId].tradable = input.dataset.tradable === 'true'
//                     } else {
//                         results[assetId][type] = value
//
//                         if (type === 'maxPrice') {
//                             results[assetId].price = value
//                         }
//
//                         if (type === 'minWM')
//                             results[assetId].minWM = value
//                     }
//                 })
//
//                 const images = document.querySelectorAll('img[data-asset]')
//                 images.forEach(img => {
//                     const assetId = img.dataset.asset;
//                     if (results[assetId]) {
//                         results[assetId].imageLink = img.src;
//
//                         const inv = img.dataset.inventory;
//                         if (inv && (!results[assetId].inventoryId || results[assetId].inventoryId.trim() === '')) {
//                             results[assetId].inventoryId = inv;
//                         }
//                     }
//                 });
//
//                 // const resultArray = Object.values(results)
//                 const resultArray = Object.values(results).filter(item => item.maxPrice && item.maxPrice.trim() !== '');
//
//                 console.log(resultArray)
//
//                 for (const item of resultArray) {
//                     const max = parseFloat(item.maxPrice)
//                     const minWM = parseFloat(item.minWM || 0)
//
//                     if (!isNaN(max) && max < minWM) {
//                         const proceed = confirm(`Для "${item.name || item.assetId}": Макс.поріг < мін.WM. Продовжити?`)
//                         if (!proceed) return;
//                     }
//                 }
//
//                 try {
//                     const response = await fetch('/api/offers/create', {
//                         method: 'POST',
//                         headers: {
//                             'Content-Type': 'application/json'
//                         },
//                         body: JSON.stringify(resultArray),
//                         signal: signal
//                     })
//
//                     if (!response.ok) {
//                         throw new Error('Помилка при відправці даних')
//                     }
//
//                     mainBlock.innerHTML = ``
//                     await sleep(2000)
//                     inventories()
//
//                     const data = await response.text()
//                     console.log(data)
//                 } catch (error) {
//                     console.error('Помилка:', error)
//                 }
//             })
//         })
// }
//
// function sleep(ms) {
//     return new Promise(resolve => setTimeout(resolve, ms));
// }

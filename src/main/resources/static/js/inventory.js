const mainBlock = document.getElementById('main-block')

export function inventories() {
    const signal = controllerInventory.signal
    fetch('/api/dmarket/get-inventories', {
        method: 'GET',
        signal: signal
    }).then(response => response.json())
        .then(data => {
            const table = document.createElement('div')
            table.className = 'table-inv'
            table.innerHTML = `
                <div></div>
                <div class="back-header"><div class="header">НАЗВА</div></div>
                <div class="back-header"><div class="header">МІН.ПОРІГ</div></div>
                <div class="back-header"><div class="header">МАКС.ПОРІГ</div></div>
                <div class="back-header"><div class="header">DM MIN <img src="/img/lock.png" alt="lock" class="lock-icon"></div></div>
                <div class="back-header"><div class="header">DM MIN <img src="/img/green lock.png" alt="lock" class="lock-icon"></div></div>
            `
            data.forEach(item => {
                table.innerHTML += `
            <div class="cell"><img src="${item.imageLink}" alt="lock" class="skin-img" data-asset="${item.assetId}" data-type="image"></div>
            <div class="cell"><input type="text" style="text-align: left;" value="${item.name}" readonly data-asset="${item.assetId}" data-type="name" data-tradable="${item.tradable}"></div>
            <div class="cell"><input type="number" value="" data-asset="${item.assetId}" data-type="minPrice"></div>
            <div class="cell"><input type="number" value="" data-asset="${item.assetId}" data-type="maxPrice"></div>
            <div class="cell"><input type="text" value="${item.skinPricesDTO.minWithLock}" readonly data-asset="${item.assetId}" data-type="locked"></div>
            <div class="cell"><input type="text" value="${item.skinPricesDTO.minWithoutLock}" readonly data-asset="${item.assetId}" data-type="unlocked"></div>
        `
            })

            mainBlock.appendChild(table)

            document.getElementById('sold').addEventListener('click', async () => {
                const inputs = document.querySelectorAll('input[data-asset]')
                const results = {}

                inputs.forEach(input => {
                    const assetId = input.dataset.asset
                    const type = input.dataset.type
                    const value = input.value

                    if (!results[assetId]) {
                        results[assetId] = {assetId}
                    }

                    if (type === 'name') {
                        results[assetId].name = value
                        results[assetId].tradable = input.dataset.tradable === 'true'
                    } else {
                        results[assetId][type] = value

                        if (type === 'maxPrice') {
                            results[assetId].price = value
                        }

                        if (type === 'locked')
                            results[assetId].minWithLock = value

                        if (type === 'unlocked')
                            results[assetId].minWithoutLock = value
                    }
                })

                const images = document.querySelectorAll('img[data-asset]')
                images.forEach(img => {
                    const assetId = img.dataset.asset
                    if (results[assetId]) {
                        results[assetId].imageLink = img.src
                    }
                })

                // const resultArray = Object.values(results)
                const resultArray = Object.values(results).filter(item => item.maxPrice && item.maxPrice.trim() !== '');

                console.log(resultArray)

                for (const item of resultArray) {
                    const max = parseFloat(item.maxPrice)
                    const minLock = parseFloat(item.minWithLock || 0)
                    const minNoLock = parseFloat(item.minWithoutLock || 0)

                    if (!isNaN(max) && max < minLock) {
                        const proceed = confirm(`Для "${item.name || item.assetId}": Макс.поріг < мін.лок. Продовжити?`)
                        if (!proceed) return;
                    } else if (!isNaN(max) && max < minNoLock) {
                        const proceed = confirm(`Для "${item.name || item.assetId}": Макс.поріг < мін.розлок. Продовжити?`)
                        if (!proceed) return
                    }
                }

                try {
                    const response = await fetch('/api/offers/create', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(resultArray),
                        signal: signal
                    })

                    if (!response.ok) {
                        throw new Error('Помилка при відправці даних')
                    }

                    mainBlock.innerHTML = ``
                    await sleep(2000)
                    inventories()

                    const data = await response.text()
                    console.log(data)
                } catch (error) {
                    console.error('Помилка:', error)
                }
            })
        })
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

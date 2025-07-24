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
        <div class="header">НАЗВА ПРЕДМЕТА</div>
        <div class="header">МІНІМАЛЬНИЙ ПОРІГ</div>
        <div class="header">МАКСИМАЛЬНИЙ ПОРІГ</div>
        <div class="header">DM МІН. ЛОК</div>
        <div class="header">DM МІН. РОЗЛОК</div>
    `
            data.forEach(item => {
                table.innerHTML += `
            <div class="cell"><input type="text" value="${item.name}" readonly data-asset="${item.assetId}" data-type="name" data-tradable="${item.tradable}"></div>
            <div class="cell"><input type="number" value="" data-asset="${item.assetId}" data-type="minPrice"></div>
            <div class="cell"><input type="number" value="" data-asset="${item.assetId}" data-type="maxPrice"></div>
            <div class="cell"><input type="text" value="${item.skinPricesDTO.minWithLock}" readonly data-asset="${item.assetId}" data-type="locked"></div>
            <div class="cell"><input type="text" value="${item.skinPricesDTO.minWithoutLock}" readonly data-asset="${item.assetId}" data-type="unlocked"></div>
        `
            })

            mainBlock.appendChild(table)

            document.getElementById('sold').addEventListener('click', () => {
                const inputs = document.querySelectorAll('input[data-asset]')
                const results = {}

                inputs.forEach(input => {
                    const assetId = input.dataset.asset
                    const type = input.dataset.type
                    const value = input.value

                    if (!results[assetId]) {
                        results[assetId] = { assetId }
                    }

                    if (type === 'name') {
                        results[assetId].name = value
                        results[assetId].tradable = input.dataset.tradable === 'true'
                    } else {
                        results[assetId][type] = value

                        if (type === 'maxPrice') {
                            results[assetId].price = value
                        }

                        if(type === 'locked')
                            results[assetId].minWithLock = value

                        if(type === 'unlocked')
                            results[assetId].minWithoutLock = value
                    }
                })

                // const resultArray = Object.values(results)
                const resultArray = Object.values(results).filter(item => item.maxPrice && item.maxPrice.trim() !== '');

                console.log(resultArray)

                if(resultArray.maxPrice < resultArray.minWithLock){
                    console.log("asdad")
                    if (confirm("Макс.поріг менший за мін.лок. Ви впевнені, що хочете продовжити?")) {
                        fetch('/api/offers/create', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(resultArray),
                            signal: signal
                        })
                            .then(async response => {
                                if (!response.ok) {
                                    throw new Error('Помилка при відправці даних')
                                }

                                mainBlock.innerHTML = ``
                                await sleep(2000);
                                inventories()

                                return response.text()
                            })
                            .then(data => {
                                console.log(data)
                            })
                            .catch(error => {
                                console.error('Помилка:', error)
                            })
                    }
                }else if(resultArray.maxPrice < resultArray.minWithoutLock){
                    if (confirm("Макс.поріг менший за мін.розлок. Ви впевнені, що хочете продовжити?")) {
                        fetch('/api/offers/create', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(resultArray),
                            signal: signal
                        })
                            .then(async response => {
                                if (!response.ok) {
                                    throw new Error('Помилка при відправці даних')
                                }

                                mainBlock.innerHTML = ``
                                await sleep(2000);
                                inventories()

                                return response.text()
                            })
                            .then(data => {
                                console.log(data)
                            })
                            .catch(error => {
                                console.error('Помилка:', error)
                            })
                    }
                }


            })
        })
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

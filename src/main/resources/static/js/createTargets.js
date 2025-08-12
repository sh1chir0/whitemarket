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
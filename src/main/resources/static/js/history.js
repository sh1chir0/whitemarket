const mainBlock = document.getElementById('main-block')

export function history() {
    const signal = controllerOffers.signal

    fetch('/api/history/get-all', {
        method: 'GET',
        signal: signal
    }).then(response => response.json())
        .then(data => {
            const table = document.createElement('div')
            table.className = 'table-history'
            table.innerHTML = `
            <div class="header">НАЗВА ПРЕДМЕТА</div>
            <div class="header">ОПЕРАЦІЯ</div>
            <div class="header">ЦІНА</div>
            <div class="header">ВИДАЛИТИ</div>
            `

            data.forEach(item => {
                table.innerHTML += `
                    <div class="cell"><input type="text" value="${item.name}" readonly data-asset="${item.id}" data-type="name"></div>
                    <div class="cell"><input type="text" value="${item.offer ? 'Куплено' : 'Продано'}" readonly data-asset="${item.id}" data-type="operationType"></div>
                    <div class="cell"><input type="number" value="${item.price}" readonly data-asset="${item.id}" data-type="maxPrice"></div>
                    <div class="cell"><button class="delete-btn" data-asset="${item.id}">🗑</button></div>
                `
            })

            mainBlock.appendChild(table)

            document.querySelectorAll('.delete-btn').forEach(button => {
                button.addEventListener('click', (e) => {
                    const id = e.target.dataset.asset;

                    fetch(`/api/history/delete/${id}`, {
                        method: 'GET',
                    })
                        .then(async response => {
                            if (!response.ok) {
                                throw new Error('Помилка при відправці даних')
                            }

                            mainBlock.innerHTML = ``
                            history()
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
        })
}
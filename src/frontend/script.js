document.getElementById('sumForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const a = e.target.a.value;
    const b = e.target.b.value;
    const res = await fetch(`/api/sum?a=${a}&b=${b}`);
    const json = await res.json();
    document.getElementById('output').textContent = JSON.stringify(json, null, 2);
});

<script>
	import QRCode from 'qrcode';

	let url = '';
	let token = '';
	let qrCodeDataUrl = '';
    let validUrl = true;

	const generateQRCode = async () => {
        if (!url || !token) {
            alert("Need to enter URL and token");
            return;
        }
		const jsonString = JSON.stringify({ url, token });
		qrCodeDataUrl = await QRCode.toDataURL(jsonString);
	};

    const isValidUrl = (input) => {
        try {
            if (input === "") return true;
            new URL(input);
            return true;
        } catch (_) {
            return false;
        }
    };

    $: (() => {
        validUrl = isValidUrl();
    }) ()
</script>
<div class="app">
    <div class="login-box">
        <h2>GlassHome QR Code Generator</h2>
        <label for="url">Home Assistant URL</label>
        <input id="url" type="text" placeholder="https://....." bind:value={url} />
        {#if !isValidUrl(url)}
            <p class="error">Not a valid URL</p>
        {/if}
        <label for="url">Long Lived API Token {#if url}- <a href={url+"/profile/security"} target="_blank">get it here</a>{/if}</label>
        <input id="token" type="text" placeholder="eyJ..." bind:value={token} />
        <button on:click={generateQRCode}>Generate QR Code</button>
        {#if qrCodeDataUrl}
            <div class="qr-code">
                <img src={qrCodeDataUrl} alt="QR Code" />
            </div>
        {/if}
    </div>    
</div>

<style>
	.app {
		display: flex;
		justify-content: center;
		align-items: center;
		height: 100vh;
		background-color: #e0f7fa;
		font-family: Arial, sans-serif;
        padding: 0;
        margin: 0;
	}

	.login-box {
		background-color: #ffffff;
		padding: 40px;
		border-radius: 10px;
		box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
		text-align: center;
	}

    .login-box label {
        display: block;
        padding-bottom: 10px;
        text-align: left;
        font-size: 13px;
    }

	.login-box input {
		display: block;
		width: 100%;
		margin-bottom: 10px;
		padding: 10px;
		border: 1px solid #ccc;
		border-radius: 5px;
	}

	.login-box button {
		background-color: #0288d1;
		color: #ffffff;
		padding: 10px 20px;
		border: none;
		border-radius: 5px;
		cursor: pointer;
	}

	.login-box button:hover {
		background-color: #0277bd;
	}

	.qr-code {
		margin-top: 20px;
        height: 100%;
        width: 100%;
	}

    .error {
        color: red;
        margin-bottom: 10px;
    }
</style>

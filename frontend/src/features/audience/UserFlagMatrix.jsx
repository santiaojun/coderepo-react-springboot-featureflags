import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { Modal } from "../../shared/components/Modal.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { attributeLabel, variationValueLabel } from "../../shared/utils/format.js";
import { reasonLabel } from "../targeting/clause-operators.js";
import { audienceApi } from "./audience.api.js";

export function UserFlagMatrix({ environmentKey, onClose, userKey }) {
    const [data, setData] = useState(null);
    const [status, setStatus] = useState({ loading: true, error: "" });

    useEffect(() => {
        let active = true;
        (async () => {
            try {
                const result = await audienceApi.flags(userKey, environmentKey);
                if (active) { setData(result); setStatus({ loading: false, error: "" }); }
            } catch (error) {
                if (active) setStatus({ loading: false, error: error.message });
            }
        })();
        return () => { active = false; };
    }, [userKey, environmentKey]);

    return createPortal(<Modal className="members-modal" label={`Flags for ${userKey}`} onClose={onClose}>
        <section className="members-drawer">
            <header className="modal-header">
                <h2>{data?.user?.name || userKey}</h2>
                <button aria-label="Close" className="icon-button" type="button" onClick={onClose}>
                    <MaterialIcon size={20}>close</MaterialIcon>
                </button>
            </header>
            <div className="members-body">
                {status.loading && <p className="panel-empty" role="status">Evaluating every flag…</p>}
                {status.error && <p className="form-error" role="alert">{status.error}</p>}
                {data && <>
                    <div className="matrix-identity">
                        <code>{data.user.key}</code>
                        <span>{data.user.email}</span>
                        <ul className="evaluation-attributes">
                            {Object.entries(data.user.attributes).map(([attribute, value]) => <li key={attribute}>
                                <span>{attributeLabel(attribute)}</span>
                                <strong>{variationValueLabel(value)}</strong>
                            </li>)}
                        </ul>
                    </div>
                    <p className="members-count">What this member receives in {data.environment.name} right now.</p>
                    <ul className="matrix-list">
                        {data.evaluations.map((entry) => <li key={entry.flagKey}>
                            <div className="matrix-flag">
                                <strong>{entry.flagName}</strong>
                                <code>{entry.flagKey}</code>
                            </div>
                            <div className="matrix-outcome">
                                <strong>{entry.variationName}</strong>
                                <code>{variationValueLabel(entry.value)}</code>
                            </div>
                            <span className={`evaluation-reason tone-${entry.reason.kind}`}>{reasonLabel(entry.reason)}</span>
                        </li>)}
                    </ul>
                </>}
            </div>
        </section>
    </Modal>, document.body);
}
